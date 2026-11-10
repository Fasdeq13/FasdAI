package com.fasdai.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fasdai.app.data.Provider
import com.fasdai.app.data.SecureKeyStore

@Composable
fun ProviderPickerScreen(
    keyStore: SecureKeyStore,
    onPicked: (Provider) -> Unit,
    onAddProvider: () -> Unit,
) {
    val configured = keyStore.configuredProviders()
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Провайдеры", style = MaterialTheme.typography.titleLarge)
        if (configured.isEmpty()) {
            Text("Ни один провайдер не настроен")
            Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onAddProvider)) {
                Text("Добавить ключ", modifier = Modifier.padding(16.dp))
            }
        }
        configured.forEach { provider ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onPicked(provider) }) {
                Text(provider.label, modifier = Modifier.padding(16.dp))
            }
        }
        if (configured.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onAddProvider)) {
                Text("Добавить ещё", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
