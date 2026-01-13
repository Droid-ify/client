package com.looker.droidify.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDataSourceFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CacheDataSourceFactory

@Module
@InstallIn(SingletonComponent::class)
@OptIn(UnstableApi::class)
object MediaModule {

    @Singleton
    @Provides
    fun provideDatabaseProvider(
        @ApplicationContext context: Context,
    ): DatabaseProvider = StandaloneDatabaseProvider(context)

    @Singleton
    @Provides
    fun provideSimpleCache(
        databaseProvider: DatabaseProvider,
        @CacheDirectory cacheDirectory: File,
    ): Cache = SimpleCache(
        cacheDirectory,
        LeastRecentlyUsedCacheEvictor(50 * 1024 * 1024 /* 50 MB */),
        databaseProvider,
    )

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .build()

    @Singleton
    @Provides
    fun provideOkHttpDataSourceFactory(
        okHttpClient: OkHttpClient,
    ): OkHttpDataSource.Factory = OkHttpDataSource.Factory(okHttpClient)

    @Singleton
    @Provides
    @DefaultDataSourceFactory
    fun provideDefaultDataSourceFactory(
        @ApplicationContext context: Context,
        okHttpDataSourceFactory: OkHttpDataSource.Factory,
    ): DataSource.Factory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)

    @Singleton
    @Provides
    @CacheDataSourceFactory
    fun provideCacheDataSourceFactory(
        cache: Cache,
        @DefaultDataSourceFactory dataSourceFactory: DataSource.Factory,
    ): DataSource.Factory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(dataSourceFactory)
}
