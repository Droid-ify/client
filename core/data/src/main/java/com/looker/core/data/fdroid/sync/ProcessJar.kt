package com.looker.core.data.fdroid.sync

import com.looker.core.common.extension.fingerprint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.fdroid.index.IndexParser
import org.fdroid.index.parseEntry
import org.fdroid.index.parseV1
import org.fdroid.index.v1.IndexV1
import org.fdroid.index.v2.Entry
import java.security.cert.X509Certificate
import java.util.jar.JarEntry
import java.util.jar.JarFile

suspend fun JarFile.getIndexType(): IndexType = withContext(Dispatchers.IO) {
	IndexType.values().first { getJarEntry(it.contentName) != null }
}

suspend fun JarFile.getIndexV1(): IndexV1 = withContext(Dispatchers.IO) {
	val indexEntry = getJarEntry(IndexType.INDEX_V1.contentName)
		?: throw ProcessJarException("Cannot find the content: ${IndexType.INDEX_V1.contentName}")
	IndexParser.parseV1(getInputStream(indexEntry))
}

suspend fun JarFile.getEntry(): Entry = withContext(Dispatchers.IO) {
	val indexEntry = getJarEntry(IndexType.ENTRY.contentName)
		?: throw ProcessJarException("Cannot find the content: ${IndexType.INDEX_V1.contentName}")
	IndexParser.parseEntry(getInputStream(indexEntry))
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
	ENTRY("entry.jar", "entry.json")
}

class ProcessJarException(override val message: String) : Exception(message)