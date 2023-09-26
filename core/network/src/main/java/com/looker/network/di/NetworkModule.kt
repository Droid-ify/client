package com.looker.network.di

import com.looker.network.Downloader
import com.looker.network.KtorDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkMap {

	@Singleton
	@Provides
	fun provideDownloader(): Downloader = KtorDownloader()

}