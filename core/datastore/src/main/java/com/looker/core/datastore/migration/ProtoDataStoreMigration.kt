package com.looker.core.datastore.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.looker.core.common.log
import com.looker.core.datastore.Settings
import kotlinx.coroutines.flow.first

class ProtoDataStoreMigration(
    private val oldDataStore: DataStore<Preferences>
) : DataMigration<Settings> {
    override suspend fun cleanUp() {
        oldDataStore.edit { it.clear() }
    }

    override suspend fun shouldMigrate(currentData: Settings): Boolean =
        oldDataStore.data.first().asMap().isNotEmpty()

    override suspend fun migrate(currentData: Settings): Settings {
        log(currentData, "SettingsMigration")
        return currentData
    }
}
