package com.fasdai.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.fasdai.app.data.Attachment
import com.fasdai.app.data.ChatMessage
import com.fasdai.app.data.Role
import com.fasdai.app.ui.ChatViewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.fasdai.app.util.encodeAttachment
import com.fasdai.app.util.highlightCode
import com.fasdai.app.util.splitMarkdownBlocks
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var pendingAttachments by remember { mutableStateOf(listOf<Attachment>()) }
    var encoding by remember { mutableStateOf(false) }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            encoding = true
            scope.launch {
                val attachment = encodeAttachment(context, uri)
                pendingAttachments = pendingAttachments + attachment
                encoding = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Код", modifier = Modifier.padding(end = 4.dp))
                Switch(checked = state.codingMode, onCheckedChange = viewModel::toggleCodingMode)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Рассуждения", modifier = Modifier.padding(end = 4.dp))
                Switch(checked = state.thinkingMode, onCheckedChange = viewModel::toggleThinkingMode)
            }
            IconButton(onClick = { viewModel.startNewChat() }) {
                Icon(Icons.Default.Add, contentDescription = "Новый чат")
            }
        }

        if (state.isCompacting) {
            Text("Сжимаю историю чата…", modifier = Modifier.padding(horizontal = 16.dp))
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.messages.filter { it.role != Role.SYSTEM }) { message ->
                MessageBubble(message)
            }
            if (state.isSending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                        IconButton(onClick = { viewModel.stopGeneration() }) {
                            Icon(Icons.Default.Stop, contentDescription = "Остановить")
                        }
                    }
                }
            }
        }

        state.errorText?.let {
            Text("Модель недоступна. $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
        }

        state.lastZip?.let { path ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp)
                    .clickableNoRipple { shareZip(context, File(path)) },
            ) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Проект собран")
                    Text(
                        "Сохранить zip",
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (pendingAttachments.isNotEmpty() || encoding) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (encoding) Text("читаю файл…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                pendingAttachments.forEach { Text(it.displayName, color = MaterialTheme.colorScheme.primary) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { pickFile.launch("*/*") }) {
                Icon(Icons.Default.AttachFile, contentDescription = "Прикрепить")
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Напиши сообщение…") },
            )
            IconButton(
                onClick = {
                    if (input.isNotBlank()) {
                        viewModel.sendMessage(input, pendingAttachments)
                        input = ""
                        pendingAttachments = emptyList()
                    }
                },
            ) {
                Icon(Icons.Default.Send, contentDescription = "Отправить")
            }
        }
    }
}

private fun shareZip(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "com.fasdai.app.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Сохранить проект"))
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(
        indication = null,
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        onClick = onClick,
    ),
)

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == Role.USER
    val bg = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Card(
            modifier = Modifier.padding(vertical = 2.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = bg),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                message.thinking?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                splitMarkdownBlocks(message.text).forEach { block ->
                    if (block.isCode) {
                        Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF0A0A0D)),
                        ) {
                            Text(
                                highlightCode(block.text, block.language),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    } else {
                        Text(block.text, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
