package com.fasdai.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fasdai.app.data.Provider
import com.fasdai.app.data.SecureKeyStore

@Composable
fun OnboardingScreen(
    keyStore: SecureKeyStore,
    onDone: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Подключи модель", style = androidx.compose.material3.MaterialTheme.typography.titleLarge) }
        item { Text("Введи ключ хотя бы одного провайдера. Ключи хранятся только на этом устройстве.") }
        items(Provider.entries.filter { it != Provider.CUSTOM }) { provider ->
            ProviderCard(provider, keyStore)
        }
        item { CustomProviderCard(keyStore) }
        item {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Продолжить")
            }
        }
    }
}

@Composable
private fun ProviderCard(provider: Provider, keyStore: SecureKeyStore) {
    var key by remember { mutableStateOf(keyStore.apiKey(provider) ?: "") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(provider.label, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = key,
                onValueChange = {
                    key = it
                    if (it.isBlank()) keyStore.clearApiKey(provider) else keyStore.setApiKey(provider, it)
                },
                label = { Text("API ключ") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CustomProviderCard(keyStore: SecureKeyStore) {
    var key by remember { mutableStateOf(keyStore.apiKey(Provider.CUSTOM) ?: "") }
    var url by remember { mutableStateOf(keyStore.customBaseUrl() ?: "") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Свой OpenAI-совместимый", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    keyStore.setCustomBaseUrl(it)
                },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = key,
                onValueChange = {
                    key = it
                    if (it.isBlank()) keyStore.clearApiKey(Provider.CUSTOM) else keyStore.setApiKey(Provider.CUSTOM, it)
                },
                label = { Text("API ключ") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
