package com.fasdai.app.data

const val COMPACT_TRIGGER_MESSAGES = 24
const val COMPACT_KEEP_RECENT = 8

fun shouldCompact(history: List<ChatMessage>): Boolean = history.size >= COMPACT_TRIGGER_MESSAGES

fun buildCompactionPrompt(history: List<ChatMessage>): String {
    val toSummarize = history.dropLast(COMPACT_KEEP_RECENT)
    val transcript = toSummarize.joinToString("\n") { "${it.role}: ${it.text}" }
    return """
Сожми переписку ниже в короткую сводку: ключевые факты, решения, договорённости и незакрытые вопросы. Пиши связно, без списка ролей, 5-10 предложений.

$transcript
""".trimIndent()
}

fun applyCompaction(history: List<ChatMessage>, summary: String): List<ChatMessage> {
    val recent = history.takeLast(COMPACT_KEEP_RECENT)
    val summaryMessage = ChatMessage(role = Role.SYSTEM, text = "Сводка предыдущей части разговора: $summary")
    return listOf(summaryMessage) + recent
}
