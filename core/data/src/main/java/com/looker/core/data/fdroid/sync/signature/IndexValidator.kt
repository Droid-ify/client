package com.looker.core.data.fdroid.sync.signature

import com.looker.core.common.signature.FileValidator
import com.looker.core.common.signature.ValidationException
import com.looker.core.data.utils.getFingerprint
import com.looker.core.data.utils.toJarFile
import com.looker.core.model.newer.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fdroid.index.SigningException
import org.fdroid.index.v1.IndexV1Verifier
import java.io.File

class IndexValidator(
	private val repo: Repo,
	private val fingerprintBlock: (String) -> Unit
) : FileValidator {
	override suspend fun validate(file: File) = withContext(Dispatchers.IO) {
		if (repo.fingerprint.isEmpty()) {
			val fingerprint = file.toJarFile()
				.getFingerprint(JSON_NAME)
				.ifEmpty { throw ValidationException("Empty Fingerprint") }
			fingerprintBlock(fingerprint)
			return@withContext
		}
		try {
			val verifier = IndexV1Verifier(jarFile = file, null, repo.fingerprint)
			val (fingerprint, _) = verifier.getStreamAndVerify { }
			fingerprintBlock(fingerprint)
		} catch (e: SigningException) {
			throw ValidationException(e.message.toString())
		}
	}

	companion object {
		const val JSON_NAME = "index-v1.json"
	}
}