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
import java.io.IOException
import java.net.SocketTimeoutException

class AnthropicClient(
    private val apiKey: String,
    private val baseUrl: String,
    private val http: OkHttpClient,
) : ChatApiClient {

    override val provider = Provider.ANTHROPIC

    override suspend fun listModels(): ModelsOutcome = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/v1/models")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .get()
            .build()
        runCatching {
            http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext ModelsOutcome.Failure(ApiError.Http(resp.code, resp.body?.string().orEmpty()))
                val json = JSONObject(resp.body!!.string())
                val arr = json.optJSONArray("data") ?: JSONArray()
                val models = (0 until arr.length()).map { i ->
                    val id = arr.getJSONObject(i).getString("id")
                    ModelInfo(id = id, provider = provider, supportsVision = true)
                }
                ModelsOutcome.Success(models)
            }
        }.getOrElse { mapException(it) }
    }

    override suspend fun send(request: ChatRequest, onCall: (okhttp3.Call) -> Unit): ApiOutcome = withContext(Dispatchers.IO) {
        val messages = JSONArray()
        request.history.filter { it.role != Role.SYSTEM }.forEach { msg ->
            val images = msg.attachments.filter { it.isImage && it.base64 != null }
            val docsNote = encodeAttachmentsNote(msg.attachments.filterNot { it.isImage })
            val content: Any = if (images.isEmpty()) {
                msg.text + docsNote
            } else {
                JSONArray().apply {
                    put(JSONObject().put("type", "text").put("text", msg.text + docsNote))
                    images.forEach { img ->
                        put(
                            JSONObject().apply {
                                put("type", "image")
                                put(
                                    "source",
                                    JSONObject().apply {
                                        put("type", "base64")
                                        put("media_type", img.mimeType)
                                        put("data", img.base64)
                                    },
                                )
                            }
                        )
                    }
                }
            }
            messages.put(
                JSONObject().apply {
                    put("role", if (msg.role == Role.USER) "user" else "assistant")
                    put("content", content)
                }
            )
        }
        val body = JSONObject().apply {
            put("model", request.model)
            put("max_tokens", 8192)
            put("messages", messages)
            if (!request.systemPrompt.isNullOrBlank()) put("system", request.systemPrompt)
        }
        val httpRequest = Request.Builder()
            .url("$baseUrl/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        runCatching {
            val call = http.newCall(httpRequest)
            onCall(call)
            call.execute().use { resp ->
                val bodyText = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext ApiOutcome.Failure(ApiError.Http(resp.code, bodyText))
                val json = JSONObject(bodyText)
                val content = json.optJSONArray("content")
                val text = buildString {
                    if (content != null) {
                        for (i in 0 until content.length()) {
                            val block = content.getJSONObject(i)
                            if (block.optString("type") == "text") append(block.optString("text"))
                        }
                    }
                }
                ApiOutcome.Success(ChatResult(text = text, thinking = null))
            }
        }.getOrElse { mapExceptionChat(it) }
    }
}

internal fun mapException(e: Throwable): ModelsOutcome = when (e) {
    is SocketTimeoutException -> ModelsOutcome.Failure(ApiError.Timeout(e.message.orEmpty()))
    is IOException -> ModelsOutcome.Failure(ApiError.Network(e.message.orEmpty()))
    else -> ModelsOutcome.Failure(ApiError.Unexpected(e.message.orEmpty()))
}

internal fun mapExceptionChat(e: Throwable): ApiOutcome = when {
    e is java.io.InterruptedIOException && e.message?.contains("Canceled", ignoreCase = true) == true -> ApiOutcome.Cancelled
    e is SocketTimeoutException -> ApiOutcome.Failure(ApiError.Timeout(e.message.orEmpty()))
    e is IOException -> ApiOutcome.Failure(ApiError.Network(e.message.orEmpty()))
    else -> ApiOutcome.Failure(ApiError.Unexpected(e.message.orEmpty()))
}
