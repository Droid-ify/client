package com.looker.droidify.di

import com.looker.droidify.data.PrivacyRepository
import com.looker.droidify.datastore.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun providePrivacyRepository(
        settingsRepository: SettingsRepository,
    ): PrivacyRepository = PrivacyRepository(
        settingsRepo = settingsRepository,
    )
}
