package com.looker.droidify.di

import com.looker.droidify.BuildConfig.BUILD_TYPE
import com.looker.droidify.BuildConfig.VERSION_NAME
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.model.ProxyPreference
import com.looker.droidify.datastore.model.ProxyType
import com.looker.droidify.network.Downloader
import com.looker.droidify.network.KtorDownloader
import com.looker.droidify.utility.common.log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import java.net.InetSocketAddress
import java.net.Proxy
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideHttpClient(settingsRepository: SettingsRepository): HttpClient {
        val proxyPreference = runBlocking { settingsRepository.getInitial().proxy }
        val engine = OkHttp.create { proxy = proxyPreference.toProxy() }
        return HttpClient(engine) {
            install(UserAgent) {
                agent = "Droid-ify/${VERSION_NAME}-${BUILD_TYPE}"
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 30_000L
                socketTimeoutMillis = 15_000L
            }
        }
    }

    @Singleton
    @Provides
    fun provideDownloader(
        httpClient: HttpClient,
        @IoDispatcher
        dispatcher: CoroutineDispatcher,
    ): Downloader = KtorDownloader(
        client = httpClient,
        dispatcher = dispatcher,
    )
}

private fun ProxyPreference.toProxy(): Proxy {
    val socketAddress = when (type) {
        ProxyType.DIRECT -> null
        ProxyType.HTTP, ProxyType.SOCKS -> {
            try {
                InetSocketAddress.createUnresolved(host, port)
            } catch (e: IllegalArgumentException) {
                log(e)
                null
            }
        }
    }
    val androidProxyType = when (type) {
        ProxyType.DIRECT -> Proxy.Type.DIRECT
        ProxyType.HTTP -> Proxy.Type.HTTP
        ProxyType.SOCKS -> Proxy.Type.SOCKS
    }
    return socketAddress?.let { Proxy(androidProxyType, it) } ?: Proxy.NO_PROXY
}
