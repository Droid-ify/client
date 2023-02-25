package com.looker.core.data.fdroid.sync

import com.looker.core.common.extension.fingerprint
import com.looker.core.data.fdroid.model.IndexV1
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.cert.X509Certificate
import java.util.jar.JarEntry
import java.util.jar.JarFile
import kotlin.coroutines.resume

// TODO: Add exact exceptions

suspend fun JarFile.getIndexV1(): IndexV1 = suspendCancellableCoroutine { cont ->
	val indexEntry = getEntry(IndexType.INDEX_V1.contentName) as JarEntry
	val indexFile = IndexV1.decodeFromInputStream(getInputStream(indexEntry))
	cont.resume(indexFile)
}

suspend fun JarFile.getFingerprint(): String = suspendCancellableCoroutine { cont ->
	val indexEntry = getEntry(IndexType.INDEX_V1.contentName) as JarEntry
	val codeSigners = indexEntry.codeSigners
	if (codeSigners == null || codeSigners.size != 1) throw Exception("index.jar must be signed by a single code signer")
	val certificates = codeSigners[0].signerCertPath?.certificates.orEmpty()
	if (certificates.size != 1) throw Exception("index.jar code signer should have only one certificate")
	val fingerprint = (certificates[0] as X509Certificate).fingerprint()
	cont.resume(fingerprint)
}

internal enum class IndexType(val jarName: String, val contentName: String) {
	INDEX_V1("index-v1.jar", "index-v1.json")
}