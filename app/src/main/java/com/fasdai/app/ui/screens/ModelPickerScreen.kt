package com.fasdai.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fasdai.app.data.ModelInfo
import com.fasdai.app.data.Provider
import com.fasdai.app.ui.ChatViewModel
import androidx.compose.foundation.clickable

@Composable
fun ModelPickerScreen(
    viewModel: ChatViewModel,
    provider: Provider,
    onPicked: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(provider) { viewModel.loadModelsFor(provider) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Выбери модель", style = MaterialTheme.typography.titleLarge)
        if (state.models.isEmpty() && state.errorText == null) {
            CircularProgressIndicator()
        }
        state.errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.models) { model: ModelInfo ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.selectModel(provider, model.id)
                        onPicked()
                    },
                ) {
                    Text(model.id, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
