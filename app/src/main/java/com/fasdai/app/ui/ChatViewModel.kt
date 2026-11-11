package com.fasdai.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fasdai.app.FasdAiApp
import com.fasdai.app.data.Attachment
import com.fasdai.app.data.ChatMessage
import com.fasdai.app.data.ModelInfo
import com.fasdai.app.data.Provider
import com.fasdai.app.data.Role
import com.fasdai.app.data.applyCompaction
import com.fasdai.app.data.buildCompactionPrompt
import com.fasdai.app.data.shouldCompact
import com.fasdai.app.net.ApiOutcome
import com.fasdai.app.net.ChatRequest
import com.fasdai.app.net.ModelsOutcome
import com.fasdai.app.util.codingSystemPrompt
import com.fasdai.app.util.parseCodeFiles
import com.fasdai.app.util.zipCodeFiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicReference

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val models: List<ModelInfo> = emptyList(),
    val selectedProvider: Provider? = null,
    val selectedModel: String? = null,
    val isSending: Boolean = false,
    val isCompacting: Boolean = false,
    val codingMode: Boolean = false,
    val thinkingMode: Boolean = false,
    val errorText: String? = null,
    val lastZip: String? = null,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<FasdAiApp>()
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var activeJob: Job? = null
    private val activeCall = AtomicReference<okhttp3.Call?>(null)
    private var currentChatId: String? = null

    init {
        viewModelScope.launch {
            val existing = app.settings.currentChatId.first()
            val id = existing ?: app.chatRepository.newChat().also { app.settings.setCurrentChatId(it) }
            currentChatId = id
            val loaded = app.chatRepository.loadMessages(id)
            if (loaded.isNotEmpty()) {
                _state.value = _state.value.copy(messages = loaded)
            }
        }
    }

    fun startNewChat() {
        viewModelScope.launch {
            val id = app.chatRepository.newChat()
            app.settings.setCurrentChatId(id)
            currentChatId = id
            _state.value = _state.value.copy(messages = emptyList(), errorText = null, lastZip = null)
        }
    }

    fun stopGeneration() {
        activeCall.get()?.cancel()
        activeJob?.cancel()
        _state.value = _state.value.copy(isSending = false, isCompacting = false)
    }

    fun selectModel(provider: Provider, model: String) {
        _state.value = _state.value.copy(selectedProvider = provider, selectedModel = model)
        viewModelScope.launch { app.settings.setSelectedModel(provider, model) }
    }

    fun toggleCodingMode(on: Boolean) {
        _state.value = _state.value.copy(codingMode = on)
    }

    fun toggleThinkingMode(on: Boolean) {
        _state.value = _state.value.copy(thinkingMode = on)
        viewModelScope.launch { app.settings.setThinkingMode(on) }
    }

    fun loadModelsFor(provider: Provider) {
        viewModelScope.launch {
            val client = app.clientFactory().clientFor(provider) ?: run {
                _state.value = _state.value.copy(errorText = "Провайдер не настроен")
                return@launch
            }
            when (val outcome = client.listModels()) {
                is ModelsOutcome.Success -> _state.value = _state.value.copy(models = outcome.models, errorText = null)
                is ModelsOutcome.Failure -> _state.value = _state.value.copy(errorText = describeError(outcome.error))
            }
        }
    }

    fun sendMessage(text: String, attachments: List<Attachment>) {
        val current = _state.value
        val provider = current.selectedProvider ?: return
        val model = current.selectedModel ?: return
        val userMessage = ChatMessage(role = Role.USER, text = text, attachments = attachments)
        val newHistory = current.messages + userMessage
        _state.value = current.copy(messages = newHistory, isSending = true, errorText = null)

        activeJob = viewModelScope.launch {
            val client = app.clientFactory().clientFor(provider) ?: run {
                _state.value = _state.value.copy(isSending = false, errorText = "Провайдер не настроен")
                return@launch
            }

            var workingHistory = newHistory
            if (shouldCompact(workingHistory)) {
                _state.value = _state.value.copy(isCompacting = true)
                val summaryRequest = ChatRequest(
                    model = model,
                    systemPrompt = null,
                    history = listOf(ChatMessage(Role.USER, buildCompactionPrompt(workingHistory))),
                    thinking = false,
                )
                val summaryOutcome = client.send(summaryRequest) { call -> activeCall.set(call) }
                if (summaryOutcome is ApiOutcome.Success) {
                    workingHistory = applyCompaction(workingHistory, summaryOutcome.result.text)
                } else if (summaryOutcome is ApiOutcome.Cancelled) {
                    _state.value = _state.value.copy(isSending = false, isCompacting = false)
                    return@launch
                }
                _state.value = _state.value.copy(isCompacting = false)
            }

            val systemPrompt = if (current.codingMode) codingSystemPrompt(text, withComments = text.contains("коммент", ignoreCase = true)) else null

            val request = ChatRequest(
                model = model,
                systemPrompt = systemPrompt,
                history = workingHistory,
                thinking = current.thinkingMode,
            )
            when (val outcome = client.send(request) { call -> activeCall.set(call) }) {
                is ApiOutcome.Cancelled -> {
                    _state.value = _state.value.copy(isSending = false)
                }
                is ApiOutcome.Success -> {
                    val assistantMessage = ChatMessage(
                        role = Role.ASSISTANT,
                        text = outcome.result.text,
                        thinking = outcome.result.thinking,
                    )
                    var zipPath: String? = null
                    if (current.codingMode) {
                        val files = parseCodeFiles(outcome.result.text)
                        if (files.isNotEmpty()) {
                            val dir = File(app.cacheDir, "exports").apply { mkdirs() }
                            val outFile = File(dir, "project_${System.currentTimeMillis()}.zip")
                            zipCodeFiles(files, outFile)
                            zipPath = outFile.absolutePath
                        }
                    }
                    _state.value = _state.value.copy(
                        messages = workingHistory + assistantMessage,
                        isSending = false,
                        lastZip = zipPath,
                    )
                    currentChatId?.let { id ->
                        app.chatRepository.saveMessage(id, userMessage)
                        app.chatRepository.saveMessage(id, assistantMessage)
                    }
                }
                is ApiOutcome.Failure -> {
                    _state.value = _state.value.copy(isSending = false, errorText = describeError(outcome.error))
                }
            }
        }
    }

    private fun describeError(error: com.fasdai.app.net.ApiError): String = when (error) {
        is com.fasdai.app.net.ApiError.Http -> "Код ${error.code}"
        is com.fasdai.app.net.ApiError.Timeout -> "Таймаут"
        is com.fasdai.app.net.ApiError.Network -> "Нет сети"
        is com.fasdai.app.net.ApiError.Unexpected -> error.detail
    }
}
