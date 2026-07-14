package com.looker.droidify.di

import com.looker.droidify.BuildConfig.BUILD_TYPE
import com.looker.droidify.BuildConfig.VERSION_NAME
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.model.ProxyPreference
import com.looker.droidify.datastore.model.ProxyType
import com.looker.droidify.network.Downloader
import com.looker.droidify.network.OkHttpDownloader
import com.looker.droidify.utility.common.log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideHttpClient(settingsRepository: SettingsRepository): OkHttpClient {
        val proxyPreference = runBlocking { settingsRepository.getInitial().proxy }
        return OkHttpClient.Builder()
            .proxy(proxyPreference.toProxy())
            .connectTimeout(30_000L, TimeUnit.MILLISECONDS)
            .readTimeout(15_000L, TimeUnit.MILLISECONDS)
            .writeTimeout(15_000L, TimeUnit.MILLISECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "Droid-ify/${VERSION_NAME}-${BUILD_TYPE}")
                        .build(),
                )
            }
            .build()
    }

    @Singleton
    @Provides
    fun provideDownloader(
        httpClient: OkHttpClient,
        @IoDispatcher
        dispatcher: CoroutineDispatcher,
    ): Downloader = OkHttpDownloader(
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
