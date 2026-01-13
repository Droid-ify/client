package com.looker.droidify.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CacheDirectory

@Module
@InstallIn(SingletonComponent::class)
object FileModule {

    @Singleton
    @Provides
    @CacheDirectory
    fun provideCacheDirectory(
        @ApplicationContext context: Context,
    ): File = context.cacheDir
}
