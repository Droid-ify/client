package com.looker.droidify.di

import android.os.Handler
import android.os.Looper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class MainHandler

@Module
@InstallIn(SingletonComponent::class)
object HandlersModule {

    @Provides
    @MainHandler
    fun providesMainHandler(): Handler = Handler(Looper.getMainLooper())
}
