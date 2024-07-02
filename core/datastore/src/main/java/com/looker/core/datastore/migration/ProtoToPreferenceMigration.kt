package com.looker.core.datastore.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.looker.core.datastore.PreferenceSettingsRepository.PreferencesKeys.setting
import com.looker.core.datastore.Settings
import kotlinx.coroutines.flow.first

class ProtoToPreferenceMigration(
    private val oldDataStore: DataStore<Settings>
) : DataMigration<Preferences> {
    override suspend fun cleanUp() {
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        return currentData.asMap().isEmpty()
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val settings = oldDataStore.data.first()
        val preferences = currentData.toMutablePreferences()
        preferences.setting(settings)
        return preferences
    }
}
