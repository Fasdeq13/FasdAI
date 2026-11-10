package com.fasdai.app.data

enum class Provider(val id: String, val label: String, val defaultBaseUrl: String) {
    ANTHROPIC("anthropic", "Anthropic Claude", "https://api.anthropic.com"),
    OPENAI("openai", "OpenAI", "https://api.openai.com"),
    GEMINI("gemini", "Google Gemini", "https://generativelanguage.googleapis.com"),
    DEEPSEEK("deepseek", "DeepSeek", "https://api.deepseek.com"),
    CUSTOM("custom", "Свой OpenAI-совместимый", "");

    companion object {
        fun byId(id: String): Provider = entries.firstOrNull { it.id == id } ?: CUSTOM
    }
}

data class ModelInfo(
    val id: String,
    val provider: Provider,
    val supportsVision: Boolean = false,
    val supportsThinking: Boolean = false,
)

data class ChatMessage(
    val role: Role,
    val text: String,
    val attachments: List<Attachment> = emptyList(),
    val thinking: String? = null,
)

enum class Role { USER, ASSISTANT, SYSTEM }

data class Attachment(
    val uri: String,
    val mimeType: String,
    val displayName: String,
)
