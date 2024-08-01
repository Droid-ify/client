package com.looker.core.domain.model

import com.looker.core.domain.model.Fingerprint.Companion.hex
import java.security.MessageDigest
import java.security.cert.Certificate
import java.util.Locale

@JvmInline
value class Fingerprint(val value: String) {

    init {
        require(value.isNotBlank() && value.length == DEFAULT_LENGTH) { "Invalid Fingerprint: $value" }
    }

    inline fun check(other: Fingerprint): Boolean {
        return other.value.equals(value, ignoreCase = true)
    }

    override fun toString(): String {
        return value.windowed(2, 2, false)
            .take(DEFAULT_LENGTH / 2).joinToString(separator = " ") { it.uppercase(Locale.US) }
    }

    internal companion object {
        const val DEFAULT_LENGTH = 64

        fun ByteArray.hex(): String = joinToString(separator = "") { byte ->
            "%02x".format(Locale.US, byte.toInt() and 0xff)
        }
    }
}

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
