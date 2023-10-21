package com.looker.core.common.signature

import com.looker.core.common.extension.exceptCancellation
import com.looker.core.common.hex
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.*

suspend fun File.verifyHash(hash: Hash): Boolean {
    return try {
        if (!hash.isValid() || !exists()) return false
        calculateHash(hash.type)
            ?.equals(hash.hash, true)
            ?: false
    } catch (e: Exception) {
        e.exceptCancellation()
        false
    }
}

suspend fun File.calculateHash(hashType: String): String? {
    return try {
        if (hashType.isBlank() || !exists()) return null
        MessageDigest
            .getInstance(hashType)
            .readBytesFrom(this)
            ?.hex()
    } catch (e: Exception) {
        e.exceptCancellation()
        null
    }
}

private suspend fun MessageDigest.readBytesFrom(
    file: File
): ByteArray? = withContext(Dispatchers.IO) {
    try {
        if (file.length() < DIRECT_READ_LIMIT) return@withContext digest(file.readBytes())
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        file.inputStream().use { input ->
            var bytesRead = input.read(buffer)
            while (bytesRead >= 0) {
                ensureActive()
                update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
            digest()
        }
    } catch (e: Exception) {
        e.exceptCancellation()
        null
    }
}

// 25 MB
private const val DIRECT_READ_LIMIT = 25 * 1024 * 1024

@Suppress("FunctionName")
data class Hash(
    val type: String,
    val hash: String
) {

    companion object {
        fun SHA256(hash: String) = Hash(type = "sha256", hash)
        fun MD5(hash: String) = Hash(type = "md5", hash)
    }

    fun isValid(): Boolean = type.isNotBlank() && hash.isNotBlank()
}
