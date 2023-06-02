package com.looker.core.data.di

import com.looker.core.data.fdroid.sync.IndexDownloader
import com.looker.core.data.fdroid.sync.IndexDownloaderImpl
import com.looker.network.Downloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.http.*

@Module
@InstallIn(SingletonComponent::class)
object DataModuleSingleton {

	@Provides
	fun provideSyncProcessor(
		downloader: Downloader
	): IndexDownloader = IndexDownloaderImpl(downloader)

}