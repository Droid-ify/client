package com.looker.droidify.sync

import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.data.model.fingerprint
import com.looker.droidify.sync.v1.model.IndexV1
import com.looker.droidify.sync.v2.model.Entry
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream

interface JarScope<out T> {
    val fingerprint: Fingerprint?
    suspend fun json(): T
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> File.toJarScope(): JarScope<T> = object : JarScope<T> {
    private val jarFile = JarFile(this@toJarScope, true)
    private var isFingerprintSet = false
    private var internalFingerprint: Fingerprint? = null

    private val entry: JarEntry by lazy {
        jarFile.getJarEntry(
            when (T::class) {
                Entry::class -> "entry.json"
                IndexV1::class -> "index-v1.json"
                else -> error("Unsupported type for parsing")
            }
        )
    }

    override val fingerprint: Fingerprint?
        get() {
            require(isFingerprintSet) { "Read the entry before reading fingerprint" }
            return internalFingerprint
        }

    override suspend fun json(): T = withContext(Dispatchers.IO) {
        try {
            jarFile.getInputStream(entry).use { stream ->
                JsonParser.decodeFromStream(stream)
            }
        } finally {
            if (!isFingerprintSet) {
                internalFingerprint = entry.fingerprint()
                isFingerprintSet = true
            }
        }
    }
}
