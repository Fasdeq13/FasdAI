package com.fasdai.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureKeyStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "fasdai_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun apiKey(provider: Provider): String? = prefs.getString(keyOf(provider), null)?.takeIf { it.isNotBlank() }

    fun setApiKey(provider: Provider, key: String) {
        prefs.edit().putString(keyOf(provider), key).apply()
    }

    fun clearApiKey(provider: Provider) {
        prefs.edit().remove(keyOf(provider)).apply()
    }

    fun customBaseUrl(): String? = prefs.getString("custom_base_url", null)

    fun setCustomBaseUrl(url: String) {
        prefs.edit().putString("custom_base_url", url).apply()
    }

    fun configuredProviders(): List<Provider> = Provider.entries.filter {
        if (it == Provider.CUSTOM) apiKey(it) != null && customBaseUrl() != null else apiKey(it) != null
    }

    private fun keyOf(provider: Provider) = "key_${provider.id}"
}
