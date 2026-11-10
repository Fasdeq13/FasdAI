package com.fasdai.app.net

sealed class ApiError(val message2: String) {
    data class Http(val code: Int, val body: String) : ApiError(describe(code))
    data class Timeout(val detail: String) : ApiError("timeout")
    data class Network(val detail: String) : ApiError("network")
    data class Unexpected(val detail: String) : ApiError(detail)

    companion object {
        fun describe(code: Int): String = when (code) {
            401 -> "401"
            403 -> "403"
            404 -> "404"
            429 -> "429"
            500 -> "500"
            502 -> "502"
            503 -> "503"
            else -> code.toString()
        }
    }
}
