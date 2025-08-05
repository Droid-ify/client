package com.looker.droidify.data.encryption

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class EncryptionStorage(
    private val datastore: DataStore<Preferences>,
    private val dispatcher: CoroutineDispatcher,
) {
    val key: Flow<Key> by lazy {
        datastore.data
            .mapLatest { preferences ->
                val bytes = preferences[KEY] ?: error("No secret key found in storage")
                Key(bytes)
            }
            .distinctUntilChanged()
            .flowOn(dispatcher)

    }

    suspend fun set(key: Key) {
        datastore.edit { it[KEY] = key.secretKey }
    }

    init {
        val scope = CoroutineScope(dispatcher)
        scope.launch {
            val preference = datastore.data.first()
            val key = preference[KEY]
            if (key == null) set(Key())
        }
    }

    private companion object {
        val KEY = byteArrayPreferencesKey("encryption_secret_key")
    }
}
