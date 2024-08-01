package com.looker.sync.fdroid.utils

import java.io.File
import java.security.CodeSigner
import java.security.cert.Certificate
import java.util.jar.JarEntry
import java.util.jar.JarFile

fun File.toJarFile(verify: Boolean = true): JarFile = JarFile(this, verify)

@get:Throws(IllegalStateException::class)
val JarEntry.codeSigner: CodeSigner
    get() = codeSigners?.singleOrNull()
        ?: error("index.jar must be signed by a single code signer, Current: $codeSigners")

@get:Throws(IllegalStateException::class)
val CodeSigner.certificate: Certificate
    get() = signerCertPath?.certificates?.singleOrNull()
        ?: error("index.jar code signer should have only one certificate")
