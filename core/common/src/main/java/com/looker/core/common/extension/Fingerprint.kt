package com.looker.core.common.extension

import com.looker.core.common.hex
import java.security.MessageDigest
import java.security.cert.Certificate

fun Certificate.fingerprint(): String {
    return runCatching { encoded.fingerprint() }.getOrElse { "" }
}

fun ByteArray.fingerprint(): String = if (size >= 256) {
    try {
        val fingerprint = MessageDigest.getInstance("sha256").digest(this)
        fingerprint.hex()
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
} else {
    ""
}
