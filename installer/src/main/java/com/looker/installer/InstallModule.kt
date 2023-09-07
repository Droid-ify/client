package com.looker.installer

import android.content.Context
import com.looker.core.datastore.SettingsRepository
import com.looker.installer.installers.shizuku.ShizukuPermissionHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InstallModule {

	@Singleton
	@Provides
	fun providesInstaller(
		@ApplicationContext context: Context,
		settingsRepository: SettingsRepository
	): InstallManager = InstallManager(context, settingsRepository)

	@Singleton
	@Provides
	fun provideShizukuPermissionHandler(
		@ApplicationContext context: Context
	): ShizukuPermissionHandler = ShizukuPermissionHandler(context)
}