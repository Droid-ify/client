package com.looker.core.domain.model

import com.looker.core.common.hex
import java.security.MessageDigest
import java.security.cert.Certificate
import java.util.Locale

@JvmInline
value class Fingerprint(val value: String) {

    init {
        if (value.length != DEFAULT_LENGTH) error("Invalid Fingerprint: $value")
    }

    inline fun isBlank(): Boolean = value.isBlank()
    inline fun isNotBlank(): Boolean = value.isNotBlank()

    inline fun check(other: Fingerprint): Boolean {
        return other.isNotBlank() && isNotBlank()
            && other.value.equals(value, ignoreCase = true)
    }

    override fun toString(): String {
        return value.windowed(2, 2, false)
            .take(DEFAULT_LENGTH / 2).joinToString(separator = " ") { it.uppercase(Locale.US) }
    }

    private companion object {
        const val DEFAULT_LENGTH = 64
    }

}

fun Certificate.fingerprint(): Fingerprint {
    val bytes = encoded
    return if (bytes.size >= 256) {
        try {
            val fingerprint = MessageDigest.getInstance("sha256").digest(bytes)
            Fingerprint(fingerprint.hex())
        } catch (e: Exception) {
            e.printStackTrace()
            Fingerprint("")
        }
    } else {
        Fingerprint("")
    }
}
