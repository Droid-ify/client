package com.looker.core_database.di

import com.looker.core_database.DroidifyDatabase
import com.looker.core_database.dao.AppDao
import com.looker.core_database.dao.RepoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

	@Provides
	fun provideAppDao(
		droidifyDatabase: DroidifyDatabase
	): AppDao = droidifyDatabase.appDao()

	@Provides
	fun provideRepoDao(
		droidifyDatabase: DroidifyDatabase
	): RepoDao = droidifyDatabase.repoDao()
}