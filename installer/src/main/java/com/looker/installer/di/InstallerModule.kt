package com.looker.installer.di

import android.content.Context
import com.looker.installer.InstallQueue
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InstallerModule {

	@Provides
	@Singleton
	fun provideInstallQueue(
		@ApplicationContext context: Context
	) : InstallQueue = InstallQueue(context)

}