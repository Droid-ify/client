package com.looker.droidify.datastore.migration

import com.looker.droidify.datastore.PreferenceSettingsRepository.PreferencesKeys.setting
import com.looker.droidify.datastore.Settings
import kotlinx.coroutines.flow.first

class ProtoToPreferenceMigration(
    private val oldDataStore: androidx.datastore.core.DataStore<Settings>
) : androidx.datastore.core.DataMigration<androidx.datastore.preferences.core.Preferences> {
    override suspend fun cleanUp() {
    }

    override suspend fun shouldMigrate(currentData: androidx.datastore.preferences.core.Preferences): Boolean {
        return currentData.asMap().isEmpty()
    }

    override suspend fun migrate(currentData: androidx.datastore.preferences.core.Preferences): androidx.datastore.preferences.core.Preferences {
        val settings = oldDataStore.data.first()
        val preferences = currentData.toMutablePreferences()
        preferences.setting(settings)
        return preferences
    }
}
