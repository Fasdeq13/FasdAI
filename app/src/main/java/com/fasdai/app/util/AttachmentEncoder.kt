package com.fasdai.app.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.fasdai.app.data.Attachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val MAX_ATTACHMENT_BYTES = 8 * 1024 * 1024

suspend fun encodeAttachment(context: Context, uri: Uri): Attachment = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri) ?: "application/octet-stream"
    val displayName = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "файл"

    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
    if (bytes == null || bytes.size > MAX_ATTACHMENT_BYTES) {
        return@withContext Attachment(uri.toString(), mimeType, displayName, base64 = null)
    }
    val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
    Attachment(uri.toString(), mimeType, displayName, base64 = encoded)
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    } catch (e: Exception) {
        null
    }
}
