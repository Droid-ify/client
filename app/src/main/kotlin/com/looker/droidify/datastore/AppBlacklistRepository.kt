package com.looker.droidify.datastore

import android.content.Context
import android.net.Uri
import com.looker.droidify.datastore.model.BlacklistEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppBlacklistRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "app_blacklist.json"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val mutex = Mutex()
    private var isLoaded = false
    private val _entries = MutableStateFlow<List<BlacklistEntry>>(emptyList())

    val entries: Flow<List<BlacklistEntry>> = flow {
        ensureLoaded()
        emitAll(_entries)
    }

    suspend fun getEntries(): List<BlacklistEntry> {
        ensureLoaded()
        return _entries.value
    }

    suspend fun addEntry(entry: BlacklistEntry): Boolean = mutex.withLock {
        ensureLoadedInternal()
        val normalized = entry.normalize()
        if (normalized.packagePattern.isBlank() && normalized.appNamePattern.isBlank()) {
            return@withLock false
        }
        val exists = _entries.value.any {
            it.packagePattern.equals(
                normalized.packagePattern,
                ignoreCase = true,
            ) && it.appNamePattern.equals(normalized.appNamePattern, ignoreCase = true)
        }
        if (exists) return@withLock false
        val newEntries = _entries.value + normalized
        saveToFile(newEntries)
        _entries.value = newEntries
        true
    }

    suspend fun updateEntry(entry: BlacklistEntry) {
        mutex.withLock {
            ensureLoadedInternal()
            val normalized = entry.normalize()
            val newEntries = _entries.value.map { if (it.id == normalized.id) normalized else it }
            saveToFile(newEntries)
            _entries.value = newEntries
        }
    }

    suspend fun removeEntry(entryId: String) {
        mutex.withLock {
            ensureLoadedInternal()
            val newEntries = _entries.value.filter { it.id != entryId }
            saveToFile(newEntries)
            _entries.value = newEntries
        }
    }

    suspend fun exportToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            ensureLoaded()
            val jsonString = json.encodeToString(
                ListSerializer(BlacklistEntry.serializer()),
                _entries.value,
            )
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            } ?: throw IllegalStateException("Cannot open output stream")
        }
    }

    suspend fun importFromUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open input stream")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val importedEntries = json.decodeFromString(
                ListSerializer(BlacklistEntry.serializer()),
                jsonString,
            ).map { it.normalize() }

            mutex.withLock {
                ensureLoadedInternal()
                val existing = _entries.value.map {
                    it.packagePattern.lowercase() to it.appNamePattern.lowercase()
                }.toSet()
                val newEntries = importedEntries.filter {
                    (it.packagePattern.lowercase() to it.appNamePattern.lowercase()) !in existing
                }
                val mergedEntries = _entries.value + newEntries
                saveToFile(mergedEntries)
                _entries.value = mergedEntries
                newEntries.size
            }
        }
    }

    private suspend fun ensureLoaded() {
        if (!isLoaded) {
            mutex.withLock {
                ensureLoadedInternal()
            }
        }
    }

    private suspend fun ensureLoadedInternal() {
        if (!isLoaded) {
            _entries.value = loadFromFile().map { it.normalize() }
                .filter { it.packagePattern.isNotBlank() || it.appNamePattern.isNotBlank() }
            isLoaded = true
        }
    }

    private suspend fun loadFromFile(): List<BlacklistEntry> = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return@withContext emptyList()
        try {
            val jsonString = file.readText()
            json.decodeFromString(ListSerializer(BlacklistEntry.serializer()), jsonString)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun saveToFile(entries: List<BlacklistEntry>) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val jsonString = json.encodeToString(
                ListSerializer(BlacklistEntry.serializer()),
                entries,
            )
            file.writeText(jsonString)
        } catch (_: Exception) {
        }
    }

    private fun BlacklistEntry.normalize(): BlacklistEntry {
        return copy(
            packagePattern = packagePattern.trim(),
            appNamePattern = appNamePattern.trim(),
        )
    }
}
