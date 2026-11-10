package com.fasdai.app.util

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

const val FILE_MARKER = "### FILE: "

fun codingSystemPrompt(userTask: String, withComments: Boolean): String {
    val commentRule = if (withComments) {
        "Комментарии в коде добавляй там, где это уместно."
    } else {
        "Не добавляй комментарии в код, кроме случаев, когда комментарий прямо запрошен в задаче."
    }
    return """
Ты работаешь в кодинг-режиме. Твоя задача — вернуть полный набор файлов проекта под задачу пользователя.

Формат ответа строго такой: перед каждым файлом отдельная строка "$FILE_MARKER относительный/путь/файла.ext", сразу после неё — блок кода в тройных кавычках с указанием языка, без лишнего текста до и после блока. Повтори это для каждого файла проекта.

Не используй другой формат. Не добавляй пояснений между файлами. $commentRule

Задача пользователя: $userTask
""".trimIndent()
}

data class CodeFile(val path: String, val content: String)

fun parseCodeFiles(response: String): List<CodeFile> {
    val result = ArrayList<CodeFile>()
    val lines = response.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        if (line.trimStart().startsWith(FILE_MARKER)) {
            val path = line.trimStart().removePrefix(FILE_MARKER).trim()
            var j = i + 1
            while (j < lines.size && !lines[j].trimStart().startsWith("```")) j++
            val fenceStart = j
            var k = fenceStart + 1
            while (k < lines.size && !lines[k].trimStart().startsWith("```")) k++
            if (fenceStart < lines.size && k <= lines.size) {
                val content = lines.subList(minOf(fenceStart + 1, lines.size), minOf(k, lines.size)).joinToString("\n")
                if (path.isNotBlank()) result.add(CodeFile(path, content))
            }
            i = k + 1
        } else {
            i++
        }
    }
    return result
}

fun zipCodeFiles(files: List<CodeFile>, outputFile: File): File {
    ZipOutputStream(outputFile.outputStream()).use { zip ->
        for (file in files) {
            zip.putNextEntry(ZipEntry(file.path))
            zip.write(file.content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }
    return outputFile
}
