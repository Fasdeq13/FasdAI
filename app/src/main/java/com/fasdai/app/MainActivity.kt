package com.fasdai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fasdai.app.data.Provider
import com.fasdai.app.ui.ChatViewModel
import com.fasdai.app.ui.screens.ChatScreen
import com.fasdai.app.ui.screens.ModelPickerScreen
import com.fasdai.app.ui.screens.OnboardingScreen
import com.fasdai.app.ui.screens.ProviderPickerScreen
import com.fasdai.app.ui.screens.SettingsScreen
import com.fasdai.app.ui.theme.ButtonShapeStyle
import com.fasdai.app.ui.theme.FasdAiTheme
import com.fasdai.app.ui.theme.accentColor

private enum class Screen { ONBOARDING, PROVIDERS, MODELS, CHAT, SETTINGS }

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as FasdAiApp

        setContent {
            var screen by remember {
                mutableStateOf(if (app.keyStore.configuredProviders().isEmpty()) Screen.ONBOARDING else Screen.PROVIDERS)
            }
            var pendingProvider by remember { mutableStateOf<Provider?>(null) }
            var bottomTab by remember { mutableIntStateOf(0) }

            LaunchedEffect(Unit) { app.rebuildHttpClient() }

            val accent by app.settings.accentColor.collectAsState(initial = 0xFF5B8CFF.toInt())
            val shapeName by app.settings.buttonShape.collectAsState(initial = "SHARP")
            val shapeStyle = runCatching { ButtonShapeStyle.valueOf(shapeName) }.getOrDefault(ButtonShapeStyle.SHARP)

            FasdAiTheme(accent = accentColor(accent), shapeStyle = shapeStyle) {
                when (screen) {
                    Screen.ONBOARDING -> OnboardingScreen(
                        keyStore = app.keyStore,
                        onDone = { screen = Screen.PROVIDERS },
                    )
                    Screen.PROVIDERS -> ProviderPickerScreen(
                        keyStore = app.keyStore,
                        onPicked = { provider ->
                            pendingProvider = provider
                            screen = Screen.MODELS
                        },
                        onAddProvider = { screen = Screen.ONBOARDING },
                    )
                    Screen.MODELS -> pendingProvider?.let { provider ->
                        ModelPickerScreen(
                            viewModel = chatViewModel,
                            provider = provider,
                            onPicked = { screen = Screen.CHAT },
                        )
                    }
                    Screen.CHAT -> Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = bottomTab == 0,
                                    onClick = { bottomTab = 0 },
                                    icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                                    label = { Text("Чат") },
                                )
                                NavigationBarItem(
                                    selected = bottomTab == 1,
                                    onClick = { bottomTab = 1 },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    label = { Text("Настройки") },
                                )
                            }
                        },
                    ) { padding ->
                        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                            if (bottomTab == 0) ChatScreen(chatViewModel) else SettingsScreen(app)
                        }
                    }
                    Screen.SETTINGS -> SettingsScreen(app)
                }
            }
        }
    }
}
