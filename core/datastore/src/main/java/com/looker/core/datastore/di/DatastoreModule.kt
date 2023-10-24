package com.looker.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.looker.core.common.Exporter
import com.looker.core.datastore.Settings
import com.looker.core.datastore.SettingsRepository
import com.looker.core.datastore.SettingsSerializer
import com.looker.core.datastore.exporter.SettingsExporter
import com.looker.core.datastore.migration.ProtoDataStoreMigration
import com.looker.core.di.ApplicationScope
import com.looker.core.di.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

private const val OLD_PREFERENCES = "preferences_file"
private const val PREFERENCES = "settings_file"

@Module
@InstallIn(SingletonComponent::class)
object DatastoreModule {

    @Singleton
    @Provides
    fun provideDatastore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile(OLD_PREFERENCES)
    }

    @Singleton
    @Provides
    fun provideProtoDatastore(
        @ApplicationContext context: Context,
        oldDataStore: DataStore<Preferences>
    ): DataStore<Settings> = DataStoreFactory.create(
        serializer = SettingsSerializer,
        migrations = listOf(
            ProtoDataStoreMigration(oldDataStore)
        )
    ) {
        context.dataStoreFile(PREFERENCES)
    }

    @Singleton
    @Provides
    fun provideSettingsExporter(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): Exporter<Settings> = SettingsExporter(
        context = context,
        scope = scope,
        ioDispatcher = dispatcher,
        json = Json {
            encodeDefaults = true
            prettyPrint = true
        }
    )

    @Singleton
    @Provides
    fun provideSettingsRepository(
        dataStore: DataStore<Settings>,
        exporter: Exporter<Settings>
    ): SettingsRepository = SettingsRepository(dataStore, exporter)
}
