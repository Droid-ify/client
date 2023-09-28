package com.looker.core.data.utils

import com.looker.core.common.extension.fingerprint
import java.io.File
import java.security.CodeSigner
import java.security.cert.Certificate
import java.util.jar.JarEntry
import java.util.jar.JarFile

internal fun File.toJarFile(verify: Boolean = true): JarFile = JarFile(this, verify)

internal fun JarFile.getFingerprint(contentName: String): String = getJarEntry(contentName)
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