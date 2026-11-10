package com.fasdai.app.net

import com.fasdai.app.data.ModelInfo
import com.fasdai.app.data.Provider
import com.fasdai.app.data.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class OpenAiCompatibleClient(
    override val provider: Provider,
    private val apiKey: String,
    private val baseUrl: String,
    private val http: OkHttpClient,
) : ChatApiClient {

    override suspend fun listModels(): ModelsOutcome = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/v1/models")
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()
        runCatching {
            http.newCall(request).execute().use { resp ->
                val bodyText = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext ModelsOutcome.Failure(ApiError.Http(resp.code, bodyText))
                val json = JSONObject(bodyText)
                val arr = json.optJSONArray("data") ?: JSONArray()
                val models = (0 until arr.length()).map { i ->
                    val id = arr.getJSONObject(i).getString("id")
                    ModelInfo(id = id, provider = provider, supportsThinking = provider == Provider.DEEPSEEK && id.contains("reasoner"))
                }
                ModelsOutcome.Success(models)
            }
        }.getOrElse { mapException(it) }
    }

    override suspend fun send(request: ChatRequest): ApiOutcome = withContext(Dispatchers.IO) {
        val messages = JSONArray()
        if (!request.systemPrompt.isNullOrBlank()) {
            messages.put(JSONObject().apply { put("role", "system"); put("content", request.systemPrompt) })
        }
        request.history.filter { it.role != Role.SYSTEM }.forEach { msg ->
            messages.put(
                JSONObject().apply {
                    put("role", if (msg.role == Role.USER) "user" else "assistant")
                    put("content", msg.text + encodeAttachmentsNote(msg.attachments))
                }
            )
        }
        val body = JSONObject().apply {
            put("model", request.model)
            put("messages", messages)
            if (provider == Provider.DEEPSEEK && request.thinking) {
                put("model", if (request.model.contains("reasoner")) request.model else "deepseek-reasoner")
            }
        }
        val httpRequest = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        runCatching {
            http.newCall(httpRequest).execute().use { resp ->
                val bodyText = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext ApiOutcome.Failure(ApiError.Http(resp.code, bodyText))
                val json = JSONObject(bodyText)
                val choice = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
                val text = choice.optString("content")
                val thinking = choice.optString("reasoning_content", null)
                ApiOutcome.Success(ChatResult(text = text, thinking = thinking))
            }
        }.getOrElse { mapExceptionChat(it) }
    }
}
