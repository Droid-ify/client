package com.looker.network.di

import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.model.ProxyType
import com.looker.network.Downloader
import com.looker.network.KtorDownloader
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.ProxyConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.URLBuilder
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

	@Provides
	@Singleton
	fun provideHttpClient(
		repository: UserPreferencesRepository
	): HttpClient {
		val preference = runBlocking { repository.fetchInitialPreferences().proxy }
		val config = when (preference.type) {
			ProxyType.DIRECT -> ProxyConfig.NO_PROXY
			ProxyType.SOCKS -> ProxyBuilder.socks(preference.host, preference.port)
			ProxyType.HTTP -> {
				val proxyUrl = URLBuilder(host = preference.host, port = preference.port).build()
				ProxyBuilder.http(proxyUrl)
			}
		}
		return HttpClient(OkHttp) {
			install(HttpTimeout) {
				connectTimeoutMillis = 30_000
				socketTimeoutMillis = 15_000
			}
			engine { proxy = config }
		}
	}

}

@Module
@InstallIn(SingletonComponent::class)
interface NetworkMap {

	@Binds
	fun bindsDownloader(
		ktorDownloader: KtorDownloader
	): Downloader

}