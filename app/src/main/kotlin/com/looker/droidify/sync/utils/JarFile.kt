package com.looker.droidify.sync.utils

import java.io.File
import java.security.CodeSigner
import java.security.cert.Certificate
import java.util.jar.JarEntry
import java.util.jar.JarFile

fun File.toJarFile(verify: Boolean = true): JarFile = JarFile(this, verify)

@get:Throws(IllegalStateException::class)
inline val JarEntry.codeSignerOrNull: CodeSigner?
    get() = codeSigners?.singleOrNull()

@get:Throws(IllegalStateException::class)
inline val JarEntry.codeSigner: CodeSigner
    get() = codeSignerOrNull
        ?: error("index.jar must be signed by a single code signer, Current: $codeSigners")

@get:Throws(IllegalStateException::class)
inline val CodeSigner.certificateOrNull: Certificate?
    get() = signerCertPath?.certificates?.singleOrNull()

@get:Throws(IllegalStateException::class)
inline val CodeSigner.certificate: Certificate
    get() = certificateOrNull
        ?: error("index.jar code signer should have only one certificate")
