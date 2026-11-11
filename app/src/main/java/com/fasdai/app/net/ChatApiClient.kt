package com.fasdai.app.net

import com.fasdai.app.data.Attachment
import com.fasdai.app.data.ChatMessage
import com.fasdai.app.data.ModelInfo
import com.fasdai.app.data.Provider
import com.fasdai.app.data.Role

data class ChatRequest(
    val model: String,
    val systemPrompt: String?,
    val history: List<ChatMessage>,
    val thinking: Boolean,
)

data class ChatResult(
    val text: String,
    val thinking: String?,
)

sealed class ApiOutcome {
    data class Success(val result: ChatResult) : ApiOutcome()
    data class Failure(val error: ApiError) : ApiOutcome()
    object Cancelled : ApiOutcome()
}

sealed class ModelsOutcome {
    data class Success(val models: List<ModelInfo>) : ModelsOutcome()
    data class Failure(val error: ApiError) : ModelsOutcome()
}

interface ChatApiClient {
    val provider: Provider
    suspend fun listModels(): ModelsOutcome
    suspend fun send(request: ChatRequest, onCall: (okhttp3.Call) -> Unit = {}): ApiOutcome
}

fun encodeAttachmentsNote(attachments: List<Attachment>): String {
    if (attachments.isEmpty()) return ""
    return attachments.joinToString(prefix = "\n", separator = "\n") { "[файл: ${it.displayName}]" }
}

fun List<ChatMessage>.withoutSystem(): List<ChatMessage> = filter { it.role != Role.SYSTEM }
