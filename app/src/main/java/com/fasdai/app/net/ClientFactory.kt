package com.fasdai.app.net

import com.fasdai.app.data.Provider
import com.fasdai.app.data.SecureKeyStore
import okhttp3.OkHttpClient

class ClientFactory(
    private val keyStore: SecureKeyStore,
    private val http: OkHttpClient,
) {
    fun clientFor(provider: Provider): ChatApiClient? {
        val key = keyStore.apiKey(provider) ?: return null
        val baseUrl = if (provider == Provider.CUSTOM) keyStore.customBaseUrl() ?: return null else provider.defaultBaseUrl
        return when (provider) {
            Provider.ANTHROPIC -> AnthropicClient(key, baseUrl, http)
            Provider.GEMINI -> GeminiClient(key, baseUrl, http)
            Provider.OPENAI, Provider.DEEPSEEK, Provider.CUSTOM -> OpenAiCompatibleClient(provider, key, baseUrl, http)
        }
    }
}

fun errorMessageRes(error: ApiError): Pair<Int, String> {
    return when (error) {
        is ApiError.Http -> when (error.code) {
            401 -> com.fasdai.app.R.string.error_401 to ""
            403 -> com.fasdai.app.R.string.error_403 to ""
            404 -> com.fasdai.app.R.string.error_404 to ""
            429 -> com.fasdai.app.R.string.error_429 to ""
            500 -> com.fasdai.app.R.string.error_500 to ""
            502 -> com.fasdai.app.R.string.error_502 to ""
            503 -> com.fasdai.app.R.string.error_503 to ""
            else -> com.fasdai.app.R.string.error_unknown to error.code.toString()
        }
        is ApiError.Timeout -> com.fasdai.app.R.string.error_timeout to ""
        is ApiError.Network -> com.fasdai.app.R.string.error_network to ""
        is ApiError.Unexpected -> com.fasdai.app.R.string.error_unknown to error.detail
    }
}
