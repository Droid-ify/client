package com.looker.core.data.fdroid.sync

import com.looker.core.common.extension.fingerprint
import com.looker.core.data.fdroid.model.v1.IndexV1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.security.cert.X509Certificate
import java.util.jar.JarEntry
import java.util.jar.JarFile

suspend fun JarFile.getIndexV1(): IndexV1 = withContext(Dispatchers.IO) {
	val indexEntry = getJarEntry(IndexType.INDEX_V1.contentName)
		?: throw ProcessJarException("Cannot find the content: ${IndexType.INDEX_V1.contentName}")
	IndexV1.decodeFromInputStream(getInputStream(indexEntry))
}

suspend fun JarFile.getFingerprint(
	indexType: IndexType = IndexType.INDEX_V1
): String = withContext(Dispatchers.IO) {
	val indexEntry = getEntry(indexType.contentName) as JarEntry
	val codeSigners = indexEntry.codeSigners
	if (codeSigners == null || codeSigners.size != 1) {
		throw ProcessJarException("index.jar must be signed by a single code signer")
	}
	yield()
	val certificates = codeSigners[0].signerCertPath?.certificates.orEmpty()
	if (certificates.size != 1) {
		throw ProcessJarException("index.jar code signer should have only one certificate")
	}
	yield()
	(certificates[0] as X509Certificate).fingerprint()
}

enum class IndexType(
	val jarName: String,
	val contentName: String
) {
	INDEX_V1("index-v1.jar", "index-v1.json"),
	INDEX_V2("index-v2.jar", "index-v2.json")
}

class ProcessJarException(override val message: String) : Exception(message)