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

class GeminiClient(
    private val apiKey: String,
    private val baseUrl: String,
    private val http: OkHttpClient,
) : ChatApiClient {

    override val provider = Provider.GEMINI

    override suspend fun listModels(): ModelsOutcome = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/v1beta/models?key=$apiKey")
            .get()
            .build()
        runCatching {
            http.newCall(request).execute().use { resp ->
                val bodyText = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext ModelsOutcome.Failure(ApiError.Http(resp.code, bodyText))
                val json = JSONObject(bodyText)
                val arr = json.optJSONArray("models") ?: JSONArray()
                val models = (0 until arr.length()).mapNotNull { i ->
                    val obj = arr.getJSONObject(i)
                    val methods = obj.optJSONArray("supportedGenerationMethods")
                    val supportsGenerate = methods != null && (0 until methods.length()).any { methods.getString(it) == "generateContent" }
                    if (!supportsGenerate) return@mapNotNull null
                    val name = obj.getString("name").removePrefix("models/")
                    ModelInfo(id = name, provider = provider, supportsVision = true)
                }
                ModelsOutcome.Success(models)
            }
        }.getOrElse { mapException(it) }
    }

    override suspend fun send(request: ChatRequest): ApiOutcome = withContext(Dispatchers.IO) {
        val contents = JSONArray()
        request.history.filter { it.role != Role.SYSTEM }.forEach { msg ->
            contents.put(
                JSONObject().apply {
                    put("role", if (msg.role == Role.USER) "user" else "model")
                    put("parts", JSONArray().put(JSONObject().put("text", msg.text + encodeAttachmentsNote(msg.attachments))))
                }
            )
        }
        val body = JSONObject().apply {
            put("contents", contents)
            if (!request.systemPrompt.isNullOrBlank()) {
                put(
                    "systemInstruction",
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", request.systemPrompt))),
                )
            }
        }
        val httpRequest = Request.Builder()
            .url("$baseUrl/v1beta/models/${request.model}:generateContent?key=$apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        runCatching {
            http.newCall(httpRequest).execute().use { resp ->
                val bodyText = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext ApiOutcome.Failure(ApiError.Http(resp.code, bodyText))
                val json = JSONObject(bodyText)
                val candidate = json.getJSONArray("candidates").getJSONObject(0)
                val parts = candidate.getJSONObject("content").getJSONArray("parts")
                val text = buildString {
                    for (i in 0 until parts.length()) append(parts.getJSONObject(i).optString("text"))
                }
                ApiOutcome.Success(ChatResult(text = text, thinking = null))
            }
        }.getOrElse { mapExceptionChat(it) }
    }
}
