package com.looker.network.di

import com.looker.network.Downloader
import com.looker.network.KtorDownloader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface NetworkMap {

	@Binds
	fun bindsDownloader(
		ktorDownloader: KtorDownloader
	): Downloader

}