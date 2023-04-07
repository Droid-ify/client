package com.looker.core.data.di

import com.looker.core.data.downloader.Downloader
import com.looker.core.data.downloader.KtorDownloader
import com.looker.core.data.fdroid.repository.AppRepository
import com.looker.core.data.fdroid.repository.RepoRepository
import com.looker.core.data.fdroid.repository.offline.OfflineFirstAppRepository
import com.looker.core.data.fdroid.repository.offline.OfflineFirstRepoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

	@Binds
	fun bindsAppRepository(
		appRepository: OfflineFirstAppRepository
	): AppRepository

	@Binds
	fun bindsRepoRepository(
		repoRepository: OfflineFirstRepoRepository
	): RepoRepository

	@Binds
	fun bindsDownloader(
		downloader: KtorDownloader
	): Downloader
}