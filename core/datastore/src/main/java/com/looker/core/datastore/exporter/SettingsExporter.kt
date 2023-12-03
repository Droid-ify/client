package com.looker.core.datastore.exporter

import android.content.Context
import android.net.Uri
import com.looker.core.common.Exporter
import com.looker.core.datastore.Settings
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

@OptIn(ExperimentalSerializationApi::class)
class SettingsExporter(
    private val context: Context,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val json: Json
) : Exporter<Settings> {

    override suspend fun export(item: Settings, target: Uri) {
        scope.launch(ioDispatcher) {
            try {
                context.contentResolver.openOutputStream(target).use {
                    if (it != null) json.encodeToStream(item, it)
                }
            } catch (e: SerializationException) {
                e.printStackTrace()
                cancel()
            } catch (e: IOException) {
                e.printStackTrace()
                cancel()
            }
        }
    }

    override suspend fun import(target: Uri): Settings = withContext(ioDispatcher) {
        try {
            context.contentResolver.openInputStream(target).use {
                if (it != null) {
                    json.decodeFromStream(it)
                } else {
                    throw IllegalStateException("Null input stream for import file")
                }
            }
        } catch (e: SerializationException) {
            e.printStackTrace()
            throw IllegalStateException(e.message)
        } catch (e: IOException) {
            e.printStackTrace()
            throw IllegalStateException(e.message)
        }
    }
}
