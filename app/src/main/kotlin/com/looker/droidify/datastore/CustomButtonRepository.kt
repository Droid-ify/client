package com.looker.droidify.datastore

import android.content.Context
import android.net.Uri
import com.looker.droidify.datastore.model.CustomButton
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
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

@Singleton
class CustomButtonRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "custom_buttons.json"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val mutex = Mutex()
    private var isLoaded = false
    private val _buttons = MutableStateFlow<List<CustomButton>>(emptyList())

    val buttons: Flow<List<CustomButton>> = flow {
        ensureLoaded()
        emitAll(_buttons)
    }

    suspend fun getButtons(): List<CustomButton> {
        ensureLoaded()
        return _buttons.value
    }

    suspend fun addButton(button: CustomButton) {
        mutex.withLock {
            ensureLoadedInternal()
            val newButtons = _buttons.value + button
            saveToFile(newButtons)
            _buttons.value = newButtons
        }
    }

    suspend fun updateButton(button: CustomButton) {
        mutex.withLock {
            ensureLoadedInternal()
            val newButtons = _buttons.value.map { if (it.id == button.id) button else it }
            saveToFile(newButtons)
            _buttons.value = newButtons
        }
    }

    suspend fun removeButton(buttonId: String) {
        mutex.withLock {
            ensureLoadedInternal()
            val newButtons = _buttons.value.filter { it.id != buttonId }
            saveToFile(newButtons)
            _buttons.value = newButtons
        }
    }

    suspend fun reorderButtons(buttons: List<CustomButton>) {
        mutex.withLock {
            saveToFile(buttons)
            _buttons.value = buttons
        }
    }

    suspend fun importFromUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open input stream")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val importedButtons = json.decodeFromString(
                ListSerializer(CustomButton.serializer()),
                jsonString
            )
            mutex.withLock {
                ensureLoadedInternal()
                val existingIds = _buttons.value.map { it.id }.toSet()
                val newButtons = importedButtons.filter { it.id !in existingIds }
                val mergedButtons = _buttons.value + newButtons
                saveToFile(mergedButtons)
                _buttons.value = mergedButtons
                newButtons.size
            }
        }
    }

    suspend fun exportToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            ensureLoaded()
            val jsonString = json.encodeToString(
                ListSerializer(CustomButton.serializer()),
                _buttons.value
            )
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            } ?: throw IllegalStateException("Cannot open output stream")
        }
    }

    fun getButtonsFile(): File {
        return File(context.filesDir, FILE_NAME)
    }

    suspend fun getButtonsJson(): String = withContext(Dispatchers.IO) {
        ensureLoaded()
        json.encodeToString(
            ListSerializer(CustomButton.serializer()),
            _buttons.value
        )
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
            _buttons.value = loadFromFile()
            isLoaded = true
        }
    }

    private suspend fun loadFromFile(): List<CustomButton> = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) {
            return@withContext emptyList()
        }
        try {
            val jsonString = file.readText()
            json.decodeFromString(
                ListSerializer(CustomButton.serializer()),
                jsonString
            )
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun saveToFile(buttons: List<CustomButton>) = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val jsonString = json.encodeToString(
                ListSerializer(CustomButton.serializer()),
                buttons
            )
            file.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
