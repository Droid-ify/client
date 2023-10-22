package com.looker.core.common

import android.util.Log
import java.util.Locale

fun <T : CharSequence> T.nullIfEmpty(): T? {
    return if (isNullOrBlank()) null else this
}

/**
 * Removes the string between the first [prefix] and last [suffix]
 *
 * For example: if "xyz_abc_123" is passed with [prefix] = "_"
 *
 * @return: "xyz_123"
 */
fun String.stripBetween(prefix: String, suffix: String = prefix): String {
    val prefixIndex = indexOf(prefix)
    val suffixIndex = lastIndexOf(suffix)
    val isRangeValid = prefixIndex != -1
        && suffixIndex != -1
        && prefixIndex != suffixIndex
    return if (isRangeValid) {
        substring(0, prefixIndex + 1) + substring(suffixIndex + 1)
    } else {
        this
    }
}

private val sizeFormats = listOf("%.0f B", "%.0f kB", "%.1f MB", "%.2f GB")

fun Long.formatSize(): String {
    val (size, index) = generateSequence(Pair(this.toFloat(), 0)) { (size, index) ->
        if (size >= 1024f) {
            Pair(size / 1024f, index + 1)
        } else {
            null
        }
    }.take(sizeFormats.size).last()
    return sizeFormats[index].format(Locale.US, size)
}

fun ByteArray.hex(): String = joinToString(separator = "") { byte ->
    "%02x".format(Locale.US, byte.toInt() and 0xff)
}

fun Any.log(
    message: Any?,
    tag: String = this::class.java.simpleName + ".DEBUG",
    type: Int = Log.DEBUG
) {
    Log.println(type, tag, message.toString())
}
