package com.looker.core.common.signature

import com.looker.core.common.extension.exceptCancellation
import com.looker.core.common.hex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

suspend fun File.verifyHash(hash: Hash): Boolean = withContext(Dispatchers.IO) {
	try {
		val digest = MessageDigest
			.getInstance(hash.name)
		if (length() < Int.MAX_VALUE) {
			return@withContext digest
				.digest(readBytes())
				.hex()
				.equals(hash.hash, ignoreCase = true)
		}
		val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
		inputStream().use { input ->
			var bytesRead = input.read(buffer)
			while (bytesRead >= 0) {
				ensureActive()
				digest.update(buffer, 0, bytesRead)
				bytesRead = input.read(buffer)
			}
			digest
				.digest()
				.hex()
				.equals(hash.hash, ignoreCase = true)
		}
	} catch (e: Exception) {
		e.exceptCancellation()
		false
	}
}

sealed class Hash(val name: String, val hash: String)

data class SHA256(val value: String) : Hash(name = "sha256", hash = value) {
	init {
		require(value.length == 64)
	}
}

data class MD5(val value: String) : Hash(name = "md5", hash = value) {
	init {
		require(value.length == 32)
	}
}