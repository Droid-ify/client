package com.looker.droidify.di

import com.looker.droidify.BuildConfig
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.model.ProxyType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

private const val CONNECTION_TIMEOUT = 30_000L
private const val READ_TIMEOUT = 15_000L
private const val USER_AGENT = "Droid-ify/${BuildConfig.VERSION_NAME}"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideOkHttpClient(
        settingsRepository: SettingsRepository,
    ): OkHttpClient {
        val proxyPreference = runBlocking { settingsRepository.getInitial().proxy }

        val proxy = when (proxyPreference.type) {
            ProxyType.DIRECT -> Proxy.NO_PROXY
            ProxyType.HTTP, ProxyType.SOCKS -> {
                val proxyType = when (proxyPreference.type) {
                    ProxyType.HTTP -> Proxy.Type.HTTP
                    ProxyType.SOCKS -> Proxy.Type.SOCKS
                    else -> Proxy.Type.DIRECT
                }
                runCatching {
                    val address = InetSocketAddress.createUnresolved(
                        proxyPreference.host,
                        proxyPreference.port
                    )
                    Proxy(proxyType, address)
                }.getOrElse { Proxy.NO_PROXY }
            }
        }

        return OkHttpClient.Builder()
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .proxy(proxy)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}
