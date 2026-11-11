package com.fasdai.app.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val keywordColor = Color(0xFF7FB4FF)
private val stringColor = Color(0xFF9FD88A)
private val commentColor = Color(0xFF6B6B78)
private val numberColor = Color(0xFFE0A85B)
private val typeColor = Color(0xFFE38FE3)
private val plainColor = Color(0xFFEDEDF2)

private val keywordsByLanguage = mapOf(
    "kotlin" to setOf(
        "fun", "val", "var", "if", "else", "when", "for", "while", "return", "class", "object",
        "interface", "import", "package", "private", "public", "internal", "override", "suspend",
        "companion", "null", "true", "false", "is", "as", "in", "try", "catch", "finally", "throw",
        "this", "super", "data", "sealed", "enum", "do", "break", "continue",
    ),
    "java" to setOf(
        "public", "private", "protected", "class", "interface", "extends", "implements", "static",
        "final", "void", "int", "long", "double", "float", "boolean", "char", "new", "return", "if",
        "else", "for", "while", "do", "switch", "case", "break", "continue", "try", "catch", "finally",
        "throw", "import", "package", "this", "super", "null", "true", "false",
    ),
    "python" to setOf(
        "def", "class", "import", "from", "as", "if", "elif", "else", "for", "while", "return",
        "yield", "try", "except", "finally", "with", "lambda", "pass", "break", "continue", "in",
        "is", "not", "and", "or", "None", "True", "False", "self", "async", "await",
    ),
    "javascript" to setOf(
        "function", "const", "let", "var", "if", "else", "for", "while", "return", "class", "extends",
        "import", "export", "default", "from", "async", "await", "try", "catch", "finally", "throw",
        "new", "this", "typeof", "null", "undefined", "true", "false", "switch", "case", "break",
    ),
    "typescript" to setOf(
        "function", "const", "let", "var", "if", "else", "for", "while", "return", "class", "extends",
        "implements", "interface", "import", "export", "default", "from", "async", "await", "try",
        "catch", "finally", "throw", "new", "this", "typeof", "null", "undefined", "true", "false",
        "type", "enum", "public", "private", "readonly",
    ),
    "cpp" to setOf(
        "int", "double", "float", "char", "void", "class", "struct", "public", "private", "protected",
        "virtual", "static", "const", "return", "if", "else", "for", "while", "do", "switch", "case",
        "break", "continue", "new", "delete", "namespace", "using", "include", "template", "typename",
        "true", "false", "nullptr", "try", "catch", "throw",
    ),
    "c" to setOf(
        "int", "double", "float", "char", "void", "struct", "static", "const", "return", "if", "else",
        "for", "while", "do", "switch", "case", "break", "continue", "typedef", "sizeof", "union",
        "enum", "include", "define",
    ),
    "rust" to setOf(
        "fn", "let", "mut", "if", "else", "match", "for", "while", "loop", "return", "struct", "enum",
        "impl", "trait", "pub", "use", "mod", "crate", "self", "Self", "true", "false", "async",
        "await", "move", "ref", "where", "unsafe",
    ),
    "go" to setOf(
        "func", "package", "import", "var", "const", "if", "else", "for", "range", "return", "struct",
        "interface", "type", "go", "chan", "select", "switch", "case", "break", "continue", "defer",
        "nil", "true", "false", "map",
    ),
    "swift" to setOf(
        "func", "var", "let", "if", "else", "for", "while", "return", "class", "struct", "enum",
        "protocol", "extension", "import", "guard", "switch", "case", "break", "continue", "true",
        "false", "nil", "self", "init", "private", "public",
    ),
    "sql" to setOf(
        "select", "from", "where", "insert", "into", "values", "update", "set", "delete", "create",
        "table", "join", "left", "right", "inner", "on", "group", "by", "order", "having", "limit",
        "and", "or", "not", "null", "as",
    ),
    "bash" to setOf(
        "if", "then", "else", "fi", "for", "while", "do", "done", "function", "return", "case", "esac",
        "echo", "export", "local",
    ),
)

private val aliasMap = mapOf(
    "kt" to "kotlin", "kts" to "kotlin",
    "js" to "javascript", "jsx" to "javascript",
    "ts" to "typescript", "tsx" to "typescript",
    "py" to "python",
    "c++" to "cpp", "cc" to "cpp", "hpp" to "cpp", "h" to "cpp",
    "rs" to "rust",
    "sh" to "bash", "shell" to "bash", "zsh" to "bash",
    "yml" to "yaml",
)

fun normalizeLanguage(raw: String): String {
    val lower = raw.trim().lowercase()
    return aliasMap[lower] ?: lower
}

private val numberRegex = Regex("""\b\d+(\.\d+)?[fFlLuU]?\b""")
private val stringRegex = Regex(""""([^"\\]|\\.)*"|'([^'\\]|\\.)*'""")
private val lineCommentRegex = Regex("""//.*$|#.*$""")
private val typeRegex = Regex("""\b[A-Z][A-Za-z0-9_]*\b""")

@Composable
fun highlightCode(code: String, language: String): AnnotatedString {
    val keywords = keywordsByLanguage[normalizeLanguage(language)]
    return buildAnnotatedString {
        code.lineSequence().forEachIndexed { index, line ->
            if (index > 0) append("\n")
            highlightLine(this, line, keywords)
        }
    }
}

private fun highlightLine(builder: androidx.compose.ui.text.AnnotatedString.Builder, line: String, keywords: Set<String>?) {
    val commentMatch = lineCommentRegex.find(line)
    val codePart = if (commentMatch != null) line.substring(0, commentMatch.range.first) else line
    val commentPart = if (commentMatch != null) line.substring(commentMatch.range.first) else ""

    var cursor = 0
    val tokenRegex = Regex(""""([^"\\]|\\.)*"|'([^'\\]|\\.)*'|\b\d+(\.\d+)?[fFlLuU]?\b|\b[A-Za-z_][A-Za-z0-9_]*\b""")
    for (match in tokenRegex.findAll(codePart)) {
        if (match.range.first > cursor) {
            builder.withStyle(SpanStyle(color = plainColor)) { builder.append(codePart.substring(cursor, match.range.first)) }
        }
        val token = match.value
        val style = when {
            stringRegex.matches(token) -> SpanStyle(color = stringColor)
            numberRegex.matches(token) -> SpanStyle(color = numberColor)
            keywords != null && token in keywords -> SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)
            token.isNotEmpty() && token[0].isUpperCase() -> SpanStyle(color = typeColor)
            else -> SpanStyle(color = plainColor)
        }
        builder.withStyle(style) { builder.append(token) }
        cursor = match.range.last + 1
    }
    if (cursor < codePart.length) {
        builder.withStyle(SpanStyle(color = plainColor)) { builder.append(codePart.substring(cursor)) }
    }
    if (commentPart.isNotEmpty()) {
        builder.withStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)) { builder.append(commentPart) }
    }
}
