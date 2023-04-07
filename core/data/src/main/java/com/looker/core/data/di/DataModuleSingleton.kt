package com.looker.core.data.di

import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.model.ProxyType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

@Module
@InstallIn(SingletonComponent::class)
object DataModuleSingleton {

	@Provides
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
			engine { proxy = config }
		}
	}
}