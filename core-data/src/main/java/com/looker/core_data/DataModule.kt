package com.looker.core_data

import com.looker.core_data.repository.AppRepository
import com.looker.core_data.repository.RepoRepository
import com.looker.core_data.repository.impl.AppRepositoryImpl
import com.looker.core_data.repository.impl.RepoRepositoryImpl
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

	fun bindAppRepository(
		appRepositoryImpl: AppRepositoryImpl
	): AppRepository

	fun bindRepoRepository(
		repoRepositoryImpl: RepoRepositoryImpl
	): RepoRepository

}