package com.looker.downloader

import android.content.Context
import com.looker.core.datastore.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
object DownloaderModule {

	@ServiceScoped
	@Provides
	fun providesDownloader(
		@ApplicationContext context: Context,
		userPreferencesRepository: UserPreferencesRepository
	): Downloader = Downloader(context, userPreferencesRepository)

}