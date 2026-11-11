package com.fasdai.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsStore by preferencesDataStore("fasdai_settings")

object SettingsKeys {
    val ACCENT_COLOR = intPreferencesKey("accent_color")
    val BUTTON_SHAPE = stringPreferencesKey("button_shape")
    val PROXY_HOST = stringPreferencesKey("proxy_host")
    val PROXY_PORT = intPreferencesKey("proxy_port")
    val CUSTOM_DNS = stringPreferencesKey("custom_dns")
    val THINKING_MODE = booleanPreferencesKey("thinking_mode")
    val SELECTED_PROVIDER = stringPreferencesKey("selected_provider")
    val SELECTED_MODEL = stringPreferencesKey("selected_model")
    val CURRENT_CHAT_ID = stringPreferencesKey("current_chat_id")
}

const val DEFAULT_ACCENT = 0xFF5B8CFF.toInt()

class AppSettingsRepo(private val context: Context) {

    val accentColor: Flow<Int> = context.settingsStore.data.map { it[SettingsKeys.ACCENT_COLOR] ?: DEFAULT_ACCENT }
    val buttonShape: Flow<String> = context.settingsStore.data.map { it[SettingsKeys.BUTTON_SHAPE] ?: "SHARP" }
    val proxyHost: Flow<String?> = context.settingsStore.data.map { it[SettingsKeys.PROXY_HOST] }
    val proxyPort: Flow<Int?> = context.settingsStore.data.map { it[SettingsKeys.PROXY_PORT] }
    val customDns: Flow<String?> = context.settingsStore.data.map { it[SettingsKeys.CUSTOM_DNS] }
    val thinkingMode: Flow<Boolean> = context.settingsStore.data.map { it[SettingsKeys.THINKING_MODE] ?: false }
    val selectedProvider: Flow<String?> = context.settingsStore.data.map { it[SettingsKeys.SELECTED_PROVIDER] }
    val selectedModel: Flow<String?> = context.settingsStore.data.map { it[SettingsKeys.SELECTED_MODEL] }
    val currentChatId: Flow<String?> = context.settingsStore.data.map { it[SettingsKeys.CURRENT_CHAT_ID] }

    suspend fun setCurrentChatId(id: String) {
        context.settingsStore.edit { it[SettingsKeys.CURRENT_CHAT_ID] = id }
    }

    suspend fun setAccentColor(color: Int) {
        context.settingsStore.edit { it[SettingsKeys.ACCENT_COLOR] = color }
    }

    suspend fun setButtonShape(style: String) {
        context.settingsStore.edit { it[SettingsKeys.BUTTON_SHAPE] = style }
    }

    suspend fun setProxy(host: String?, port: Int?) {
        context.settingsStore.edit {
            if (host.isNullOrBlank()) it.remove(SettingsKeys.PROXY_HOST) else it[SettingsKeys.PROXY_HOST] = host
            if (port == null) it.remove(SettingsKeys.PROXY_PORT) else it[SettingsKeys.PROXY_PORT] = port
        }
    }

    suspend fun setCustomDns(dns: String?) {
        context.settingsStore.edit {
            if (dns.isNullOrBlank()) it.remove(SettingsKeys.CUSTOM_DNS) else it[SettingsKeys.CUSTOM_DNS] = dns
        }
    }

    suspend fun setThinkingMode(on: Boolean) {
        context.settingsStore.edit { it[SettingsKeys.THINKING_MODE] = on }
    }

    suspend fun setSelectedModel(provider: Provider, model: String) {
        context.settingsStore.edit {
            it[SettingsKeys.SELECTED_PROVIDER] = provider.id
            it[SettingsKeys.SELECTED_MODEL] = model
        }
    }
}
