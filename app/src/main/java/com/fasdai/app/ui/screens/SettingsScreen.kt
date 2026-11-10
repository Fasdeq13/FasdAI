package com.fasdai.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fasdai.app.FasdAiApp
import com.fasdai.app.ui.theme.ButtonShapeStyle
import kotlinx.coroutines.launch

private val presetColors = listOf(
    0xFF5B8CFF.toInt(),
    0xFFFF4D6D.toInt(),
    0xFF33D17A.toInt(),
    0xFFFFC107.toInt(),
    0xFF8B5CF6.toInt(),
    0xFFFFFFFF.toInt(),
)

@Composable
fun SettingsScreen(app: FasdAiApp) {
    val scope = rememberCoroutineScope()
    val accent by app.settings.accentColor.collectAsState(initial = presetColors[0])
    val shape by app.settings.buttonShape.collectAsState(initial = "SHARP")
    val proxyHost by app.settings.proxyHost.collectAsState(initial = null)
    val proxyPort by app.settings.proxyPort.collectAsState(initial = null)
    val dns by app.settings.customDns.collectAsState(initial = null)

    var hostField by remember(proxyHost) { mutableStateOf(proxyHost ?: "") }
    var portField by remember(proxyPort) { mutableStateOf(proxyPort?.toString() ?: "") }
    var dnsField by remember(dns) { mutableStateOf(dns ?: "") }

    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Настройки", style = MaterialTheme.typography.titleLarge)

        Text("Акцентный цвет")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            presetColors.forEach { color ->
                val selected = color == accent
                Column(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        .clickable { scope.launch { app.settings.setAccentColor(color) } },
                ) {}
            }
        }

        Text("Форма кнопок")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                ButtonShapeStyle.SHARP to "Жёсткая",
                ButtonShapeStyle.ROUNDED to "Скруглённая",
                ButtonShapeStyle.PILL to "Капсула",
            ).forEach { (style, label) ->
                Button(
                    onClick = { scope.launch { app.settings.setButtonShape(style.name) } },
                    colors = if (style.name == shape) androidx.compose.material3.ButtonDefaults.buttonColors() else androidx.compose.material3.ButtonDefaults.outlinedButtonColors(),
                ) {
                    Text(label)
                }
            }
        }

        Text("Сеть", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = hostField, onValueChange = { hostField = it }, label = { Text("Прокси хост") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = portField, onValueChange = { portField = it }, label = { Text("Прокси порт") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = dnsField, onValueChange = { dnsField = it }, label = { Text("Свой DNS") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                scope.launch {
                    app.settings.setProxy(hostField.ifBlank { null }, portField.toIntOrNull())
                    app.settings.setCustomDns(dnsField.ifBlank { null })
                    app.rebuildHttpClient()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить")
        }
    }
}
