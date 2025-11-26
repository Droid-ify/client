package com.looker.droidify.sync

import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.data.model.check
import com.looker.droidify.network.validation.invalid
import com.looker.droidify.sync.v1.model.IndexV1
import com.looker.droidify.sync.v2.model.Entry
import java.util.jar.JarFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

val JsonParser = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

suspend inline fun <reified T> JarFile.parseJson(
    fingerprint: Fingerprint? = null,
): Pair<Fingerprint, T> = withContext(Dispatchers.IO) {
    val entryName = when (T::class) {
        Entry::class -> "entry.json"
        IndexV1::class -> "index-v1.json"
        else -> error("Unsupported type for parsing")
    }

    val entry = getJarEntry(entryName)

    val entryString = getInputStream(entry).use {
        it.readBytes().decodeToString()
    }

    val jarFingerprint = requireNotNull(Fingerprint(entry)) {
        "Jar entry does not contain a fingerprint"
    }

    if (fingerprint != null && !fingerprint.check(jarFingerprint)) {
        invalid("Expected fingerprint: $fingerprint, Actual fingerprint: $jarFingerprint")
    }

    (fingerprint ?: jarFingerprint) to JsonParser.decodeFromString(entryString)
}
