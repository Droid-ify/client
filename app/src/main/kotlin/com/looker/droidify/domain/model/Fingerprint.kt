package com.looker.droidify.domain.model

import java.security.MessageDigest
import java.security.cert.Certificate
import java.util.*

@JvmInline
value class Fingerprint(val value: String) {

    val isValid: Boolean
        get() = value.isNotBlank() && value.length == FINGERPRINT_LENGTH

    override fun toString(): String = value
}

@Suppress("NOTHING_TO_INLINE")
inline fun Fingerprint.check(found: Fingerprint): Boolean {
    return found.value.equals(value, ignoreCase = true)
}

private const val FINGERPRINT_LENGTH = 64

fun ByteArray.hex(): String = joinToString(separator = "") { byte ->
    "%02x".format(Locale.US, byte.toInt() and 0xff)
}

fun Fingerprint.formattedString(): String = value.windowed(2, 2, false)
    .take(FINGERPRINT_LENGTH / 2).joinToString(separator = " ") { it.uppercase(Locale.US) }

fun String.fingerprint(): Fingerprint = Fingerprint(
    MessageDigest.getInstance("SHA-256")
        .digest(
            this
                .chunked(2)
                .mapNotNull { byteStr ->
                    try {
                        byteStr.toInt(16).toByte()
                    } catch (_: NumberFormatException) {
                        null
                    }
                }
                .toByteArray(),
        ).hex(),
)

fun Certificate.fingerprint(): Fingerprint {
    val bytes = encoded
    return if (bytes.size >= 256) {
        try {
            val fingerprint = MessageDigest.getInstance("sha256").digest(bytes)
            Fingerprint(fingerprint.hex().uppercase())
        } catch (e: Exception) {
            e.printStackTrace()
            Fingerprint("")
        }
    } else {
        Fingerprint("")
    }
}
