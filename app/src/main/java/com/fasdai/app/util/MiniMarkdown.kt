package com.fasdai.app.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

data class MdBlock(val isCode: Boolean, val language: String, val text: String)

fun splitMarkdownBlocks(text: String): List<MdBlock> {
    val blocks = ArrayList<MdBlock>()
    val lines = text.lines()
    var i = 0
    val buffer = StringBuilder()
    fun flush() {
        if (buffer.isNotEmpty()) {
            blocks.add(MdBlock(false, "", buffer.toString().trim('\n')))
            buffer.clear()
        }
    }
    while (i < lines.size) {
        val line = lines[i]
        if (line.trimStart().startsWith("```")) {
            flush()
            val lang = line.trim().removePrefix("```").trim()
            val codeLines = ArrayList<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MdBlock(true, lang, codeLines.joinToString("\n")))
            i++
        } else {
            buffer.append(line).append("\n")
            i++
        }
    }
    flush()
    return blocks
}

@Composable
fun inlineMarkdown(line: String) = buildAnnotatedString {
    var remaining = line
    while (remaining.isNotEmpty()) {
        val boldIdx = remaining.indexOf("**")
        val codeIdx = remaining.indexOf("`")
        when {
            boldIdx == 0 -> {
                val end = remaining.indexOf("**", 2)
                if (end > 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(remaining.substring(2, end)) }
                    remaining = remaining.substring(end + 2)
                } else {
                    append(remaining); remaining = ""
                }
            }
            codeIdx == 0 -> {
                val end = remaining.indexOf("`", 1)
                if (end > 0) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = MaterialTheme.colorScheme.surfaceVariant)) {
                        append(remaining.substring(1, end))
                    }
                    remaining = remaining.substring(end + 1)
                } else {
                    append(remaining); remaining = ""
                }
            }
            boldIdx < 0 && codeIdx < 0 -> {
                append(remaining); remaining = ""
            }
            else -> {
                val cut = listOf(boldIdx, codeIdx).filter { it >= 0 }.min()
                append(remaining.substring(0, cut))
                remaining = remaining.substring(cut)
            }
        }
    }
}

@Composable
fun headingStyle(line: String): Pair<String, FontWeight> = when {
    line.startsWith("### ") -> line.removePrefix("### ") to FontWeight.Bold
    line.startsWith("## ") -> line.removePrefix("## ") to FontWeight.ExtraBold
    line.startsWith("# ") -> line.removePrefix("# ") to FontWeight.Black
    else -> line to FontWeight.Normal
}
