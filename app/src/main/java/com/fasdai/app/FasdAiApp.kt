package com.fasdai.app

import android.app.Application
import com.fasdai.app.data.AppSettingsRepo
import com.fasdai.app.data.ChatRepository
import com.fasdai.app.data.FasdAiDatabase
import com.fasdai.app.data.SecureKeyStore
import com.fasdai.app.net.ClientFactory
import com.fasdai.app.net.buildHttpClient
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient

class FasdAiApp : Application() {

    lateinit var keyStore: SecureKeyStore
        private set
    lateinit var settings: AppSettingsRepo
        private set
    lateinit var chatRepository: ChatRepository
        private set

    @Volatile
    private var cachedHttp: OkHttpClient? = null

    override fun onCreate() {
        super.onCreate()
        keyStore = SecureKeyStore(this)
        settings = AppSettingsRepo(this)
        chatRepository = ChatRepository(FasdAiDatabase.get(this).chatDao())
    }

    suspend fun rebuildHttpClient() {
        val host = settings.proxyHost.first()
        val port = settings.proxyPort.first()
        val dns = settings.customDns.first()
        cachedHttp = buildHttpClient(host, port, dns)
    }

    fun clientFactory(): ClientFactory {
        val http = cachedHttp ?: buildHttpClient(null, null, null).also { cachedHttp = it }
        return ClientFactory(keyStore, http)
    }
}
