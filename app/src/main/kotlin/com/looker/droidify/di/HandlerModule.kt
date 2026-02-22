package com.looker.droidify.di

import android.content.Context
import com.looker.droidify.data.StringHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object HandlersModule {
    @Provides
    @ViewModelScoped
    fun provideStringHandler(
        @ApplicationContext context: Context,
    ): StringHandler = StringHandler(context)
}
