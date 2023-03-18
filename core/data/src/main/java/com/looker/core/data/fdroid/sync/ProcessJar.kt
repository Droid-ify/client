package com.looker.core.data.fdroid.sync

import com.looker.core.common.extension.fingerprint
import com.looker.core.data.fdroid.model.v1.IndexV1
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.cert.X509Certificate
import java.util.jar.JarEntry
import java.util.jar.JarFile
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun JarFile.getIndexV1(): IndexV1 = suspendCancellableCoroutine { cont ->
	val indexEntry = getEntry(IndexType.INDEX_V1.contentName) as JarEntry
	val indexFile = IndexV1.decodeFromInputStream(getInputStream(indexEntry))
	cont.resume(indexFile)
}

suspend fun JarFile.getFingerprint(
	indexType: IndexType = IndexType.INDEX_V1
): String = suspendCancellableCoroutine { cont ->
	val indexEntry = getEntry(indexType.contentName) as JarEntry
	val codeSigners = indexEntry.codeSigners
	if (codeSigners == null || codeSigners.size != 1) {
		cont.resumeWithException(
			ProcessJarException("index.jar must be signed by a single code signer")
		)
	}
	if (cont.isCompleted) return@suspendCancellableCoroutine
	val certificates = codeSigners[0].signerCertPath?.certificates.orEmpty()
	if (certificates.size != 1) {
		cont.resumeWithException(
			ProcessJarException("index.jar code signer should have only one certificate")
		)
	}
	if (cont.isCompleted) return@suspendCancellableCoroutine
	val fingerprint = (certificates[0] as X509Certificate).fingerprint()
	cont.resume(fingerprint)
}

enum class IndexType(
	val jarName: String,
	val contentName: String
) {
	INDEX_V1("index-v1.jar", "index-v1.json"),
	INDEX_V2("index-v2.jar", "index-v2.json")
}

class ProcessJarException(override val message: String) : Exception(message)