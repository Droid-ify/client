package com.looker.droidify.di

import com.looker.droidify.network.Downloader
import com.looker.droidify.network.KtorDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideDownloader(
        @IoDispatcher
        dispatcher: CoroutineDispatcher,
    ): Downloader = KtorDownloader(
        httpClientEngine = OkHttp.create(),
        dispatcher = dispatcher,
    )
}
