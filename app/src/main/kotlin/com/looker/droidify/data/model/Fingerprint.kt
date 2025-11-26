package com.looker.droidify.data.model

import com.looker.droidify.sync.utils.certificateOrNull
import com.looker.droidify.sync.utils.codeSignerOrNull
import java.security.MessageDigest
import java.security.cert.Certificate
import java.util.*
import java.util.jar.JarEntry

@JvmInline
value class Fingerprint(val value: String) {

    val isValid: Boolean
        get() = value.isNotBlank() && value.length == FINGERPRINT_LENGTH

    override fun toString(): String = value
}

/**
 * Creates a [Fingerprint] from a given certificate.
 *
 * This function calculates the SHA-256 hash of the certificate's encoded bytes.
 * It returns an empty fingerprint if the certificate bytes are too small or if
 * an error occurs during the hashing process.
 *
 * @return A [Fingerprint] object representing the SHA-256 hash of the certificate,
 *         or null on failure or invalid certificate
 */
fun Fingerprint(entry: JarEntry): Fingerprint? =
    entry.codeSignerOrNull?.certificateOrNull?.fingerprint().takeIf { it?.isValid == true }

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
