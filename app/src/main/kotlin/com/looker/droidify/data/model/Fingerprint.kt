@file:Suppress("NOTHING_TO_INLINE")

package com.looker.droidify.data.model

import com.looker.droidify.data.encryption.sha256
import com.looker.droidify.sync.utils.certificateOrNull
import com.looker.droidify.sync.utils.codeSignerOrNull
import java.security.cert.Certificate
import java.util.*
import java.util.jar.JarEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JvmInline
value class Fingerprint(val value: String) {

    inline val isValid: Boolean
        get() = value.isNotBlank() && value.length == Length

    inline fun assert(other: Fingerprint): Boolean = other.value.equals(value, ignoreCase = true)

    override fun toString(): String = value

    companion object {
        const val Length = 64
    }
}

suspend inline fun JarEntry.fingerprint(): Fingerprint? = withContext(Dispatchers.IO) {
    codeSignerOrNull?.certificateOrNull?.fingerprint()
}

inline fun Certificate.fingerprint(): Fingerprint? {
    val bytes = this.encoded.takeIf { it.size >= 256 } ?: return null
    return Fingerprint(sha256(bytes).hex().uppercase()).takeIf { it.isValid }
}

inline fun ByteArray.hex(): String = joinToString(separator = "") { byte ->
    "%02x".format(Locale.US, byte.toInt() and 0xff)
}

inline fun Fingerprint.formattedString(): String = value.chunked(2)
    .take(Fingerprint.Length / 2).joinToString(separator = " ") { it.uppercase(Locale.US) }
