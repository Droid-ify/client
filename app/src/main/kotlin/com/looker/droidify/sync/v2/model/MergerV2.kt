package com.looker.droidify.sync.v2.model

import android.util.Log
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.io.InputStream

/**
 * Merger for applying JSON Merge Patch (RFC 7386) to IndexV2 instances.
 * Adapted from Neo Store.
 */
class IndexV2Merger(private val baseFile: File) : AutoCloseable {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun getCurrentIndex(): IndexV2? = json.decodeFromStream(baseFile.inputStream())

    fun processDiff(
        diffStream: InputStream,
    ): Boolean {
        val tempFile = File.createTempFile("merged_", ".json")

        try {
            val hasChanged = merge(baseFile, diffStream, tempFile)

            if (hasChanged) {
                // Save the merged result
                tempFile.copyTo(baseFile, overwrite = true)
                tempFile.inputStream().use { inputStream ->
                    val mergedElement = json.decodeFromStream<JsonElement>(inputStream)
                    val timestamp = getTimestamp(mergedElement)
                    baseFile.setLastModified(timestamp)
                }
            }

            Log.d("IndexV2Merger", "Merged a diff JSON into the base: $hasChanged")
            return hasChanged
        } finally {
            tempFile.delete()
        }
    }

    private fun merge(baseFile: File, diffStream: InputStream, outputFile: File): Boolean {
        val baseElement =
            runCatching { baseFile.inputStream().use { json.decodeFromStream<JsonElement>(it) } }
                .fold(
                    onSuccess = { it },
                    onFailure = {
                        throw Exception(it.message.orEmpty(), it)
                    },
                )
        val diffElement = runCatching { json.decodeFromStream<JsonElement>(diffStream) }
            .fold(
                onSuccess = { it },
                onFailure = {
                    throw Exception(it.message.orEmpty(), it)
                },
            )

        // No need to apply a diff older or same as base
        val baseTimestamp = getTimestamp(baseElement)
        val diffTimestamp = getTimestamp(diffElement)

        if (diffTimestamp <= baseTimestamp) {
            baseFile.copyTo(outputFile, overwrite = true)
            return false
        }

        // Apply the merge patch
        val mergedElement = mergePatch(baseElement, diffElement)

        // Ensure the timestamp is updated
        val mergedObj = mergedElement.jsonObject.toMutableMap()
        val repoObj = (mergedObj["repo"] as? JsonObject)?.toMutableMap() ?: run {
            baseFile.copyTo(outputFile, overwrite = true)
            return false
        }

        repoObj["timestamp"] = JsonPrimitive(diffTimestamp)
        val finalResult = JsonObject(mergedObj + ("repo" to JsonObject(repoObj)))

        outputFile.outputStream().use { outputStream ->
            json.encodeToStream(finalResult, outputStream)
        }

        return baseElement != finalResult
    }

    /**
     * Applies a JSON Merge Patch (RFC 7386) to the target JSON element. RFC 7386 rules:
     * - If patch is not an object, replace target entirely
     * - If patch value is null, remove the key from target
     * - If patch value is an object, recursively merge with target
     * - Otherwise, replace target value with patch value
     */
    private fun mergePatch(target: JsonElement, patch: JsonElement): JsonElement {
        if (patch !is JsonObject || target !is JsonObject) return patch
        val result = target.jsonObject.toMutableMap()

        for ((key, value) in patch) {
            // No change when object is empty
            if (value is JsonObject && value.jsonObject.isEmpty()) continue

            when (value) {
                // Remove null objects
                is JsonNull   -> {
                    result.remove(key)
                }

                // Recursively merge objects
                is JsonObject -> {
                    val targetValue = target.jsonObject[key]
                    result[key] = if (targetValue is JsonObject) {
                        mergePatch(targetValue, value)
                    } else {
                        // If target doesn't have this key or it's not an object, use the patch value
                        value
                    }
                }

                // Replace primitive values entirely
                else          -> {
                    result[key] = value
                }
            }
        }

        return JsonObject(result)
    }

    private fun getTimestamp(element: JsonElement): Long {
        return (element.jsonObject["repo"]?.jsonObject?.get("timestamp") as? JsonPrimitive)?.longOrNull
            ?: 0L
    }

    override fun close() {
        // Cleanup when needed
    }
}
