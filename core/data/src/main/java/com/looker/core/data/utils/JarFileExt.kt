package com.looker.core.data.utils

import com.looker.core.common.extension.fingerprint
import com.looker.core.common.extension.writeTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.CodeSigner
import java.security.cert.Certificate
import java.util.jar.JarEntry
import java.util.jar.JarFile

internal fun File.toJarFile(verify: Boolean = true): JarFile = JarFile(this, verify)

internal fun JarFile.getEntryStream(contentName: String): InputStream =
	getInputStream(getJarEntry(contentName))

internal fun JarFile.getFingerprint(contentName: String): String = getJarEntry(contentName)
	// TODO: Will improve this in future
	.apply {
		val deadFile = File.createTempFile("dead", System.currentTimeMillis().toString())
		runBlocking {
			withContext(Dispatchers.IO) {
				getInputStream(this@apply).writeTo(deadFile)
			}
		}
		deadFile.delete()
	}
	.codeSigner
	.certificate
	.fingerprint()

@get:Throws(IllegalStateException::class)
internal val JarEntry.codeSigner: CodeSigner
	get() = codeSigners?.singleOrNull()
		?: throw IllegalStateException("index.jar must be signed by a single code signer")

@get:Throws(IllegalStateException::class)
internal val CodeSigner.certificate: Certificate
	get() = signerCertPath?.certificates?.singleOrNull()
		?: throw IllegalStateException("index.jar code signer should have only one certificate")