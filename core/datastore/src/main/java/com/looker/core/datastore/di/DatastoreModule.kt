package com.looker.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.looker.core.datastore.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val PREFERENCES = "preferences_file"

@Module
@InstallIn(SingletonComponent::class)
object DatastoreModule {

	@Singleton
	@Provides
	fun provideDatastore(
		@ApplicationContext context: Context
	): DataStore<Preferences> = PreferenceDataStoreFactory.create {
		context.preferencesDataStoreFile(PREFERENCES)
	}

	@Singleton
	@Provides
	fun provideUserPreferencesRepository(
		dataStore: DataStore<Preferences>
	): UserPreferencesRepository = UserPreferencesRepository(dataStore)
}