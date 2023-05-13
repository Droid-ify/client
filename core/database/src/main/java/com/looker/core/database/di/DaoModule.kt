package com.looker.core.database.di

import com.looker.core.database.DroidifyDatabase
import com.looker.core.database.dao.AppDao
import com.looker.core.database.dao.InstalledDao
import com.looker.core.database.dao.RepoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

	@Provides
	@Singleton
	fun provideRepoDao(
		database: DroidifyDatabase
	): RepoDao = database.repoDao()

	@Provides
	@Singleton
	fun provideAppDao(
		database: DroidifyDatabase
	): AppDao = database.appDao()

	@Provides
	@Singleton
	fun provideInstalledDao(
		database: DroidifyDatabase
	): InstalledDao = database.installedDao()

}