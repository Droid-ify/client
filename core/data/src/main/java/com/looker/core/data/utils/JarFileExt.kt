package com.looker.core.data.utils

import java.security.CodeSigner
import java.security.cert.Certificate
import java.util.jar.JarEntry

@get:Throws(IllegalStateException::class)
internal val JarEntry.codeSigner: CodeSigner
	get() = codeSigners?.singleOrNull()
		?: throw IllegalStateException("index.jar must be signed by a single code signer")

@get:Throws(IllegalStateException::class)
internal val CodeSigner.certificate: Certificate
	get() = signerCertPath?.certificates?.singleOrNull()
		?: throw IllegalStateException("index.jar code signer should have only one certificate")