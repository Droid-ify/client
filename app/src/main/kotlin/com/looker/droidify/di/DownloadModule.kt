package com.looker.droidify.di

import android.content.Context
import com.looker.droidify.network.Downloader
import com.looker.droidify.service.DownloadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {

    @Singleton
    @Provides
    fun providesDownloadManager(
        @ApplicationContext context: Context,
        downloader: Downloader,
    ): DownloadManager = DownloadManager(context, downloader)
}
