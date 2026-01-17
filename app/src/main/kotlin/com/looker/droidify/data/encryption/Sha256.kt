package com.looker.droidify.data.encryption

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

private val DIGEST = MessageDigest.getInstance("SHA-256")

fun sha256(data: String): ByteArray = DIGEST.digest(data.toByteArray())

fun sha256(data: ByteArray): ByteArray = DIGEST.digest(data)

fun sha256(data: File): ByteArray = data.inputStream().use(::sha256)

fun sha256(data: InputStream): ByteArray = synchronized(DIGEST) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var bytesRead = data.read(buffer)
    while (bytesRead >= 0) {
        DIGEST.update(buffer, 0, bytesRead)
        bytesRead = data.read(buffer)
    }
    DIGEST.digest()
}
