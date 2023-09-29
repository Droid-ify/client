package com.looker.core.data.fdroid.sync.signature

import com.looker.core.common.extension.exceptCancellation
import com.looker.core.common.signature.FileValidator
import com.looker.core.common.signature.ValidationException
import com.looker.core.data.utils.getFingerprint
import com.looker.core.data.utils.toJarFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class IndexValidator(
	private val fingerprintBlock: (String) -> Unit
) : FileValidator {
	override suspend fun validate(file: File) = withContext(Dispatchers.IO) {
		try {
			val fingerprint = file.toJarFile()
				.getFingerprint(JSON_NAME)
				.ifEmpty { throw ValidationException("Empty Fingerprint") }
			fingerprintBlock(fingerprint)
		} catch (e: Exception) {
			e.exceptCancellation()
			throw ValidationException(e.message.toString())
		}
	}

	companion object {
		const val JSON_NAME = "index-v1.json"
	}
}