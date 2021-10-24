@file:Suppress("PackageDirectoryMismatch")

package com.looker.droidify.utility.extension.text

import android.util.Log
import java.util.*

fun <T : CharSequence> T.nullIfEmpty(): T? {
    return if (isNullOrEmpty()) null else this
}

private val sizeFormats = listOf("%.0f B", "%.0f kB", "%.1f MB", "%.2f GB")

fun Long.formatSize(): String {
    val (size, index) = generateSequence(Pair(this.toFloat(), 0)) { (size, index) ->
        if (size >= 1024f)
            Pair(size / 1024f, index + 1) else null
    }.take(sizeFormats.size).last()
    return sizeFormats[index].format(Locale.US, size)
}

fun String?.trimAfter(char: Char, repeated: Int): String? {
    var count = 0
    this?.let {
        for (i in it.indices) {
            if (it[i] == char) count++
            if (repeated == count) return it.substring(0, i)
        }
    }
    return null
}

fun String?.trimBefore(char: Char, repeated: Int): String? {
    var count = 0
    this?.let {
        for (i in it.indices) {
            if (it[i] == char) count++
            if (repeated == count) return it.substring(i+1)
        }
    }
    return null
}

fun Char.halfByte(): Int {
    return when (this) {
        in '0'..'9' -> this - '0'
        in 'a'..'f' -> this - 'a' + 10
        in 'A'..'F' -> this - 'A' + 10
        else -> -1
    }
}

fun CharSequence.unhex(): ByteArray? {
    return if (length % 2 == 0) {
        val ints = windowed(2, 2, false).map {
            val high = it[0].halfByte()
            val low = it[1].halfByte()
            if (high >= 0 && low >= 0) {
                (high shl 4) or low
            } else {
                -1
            }
        }
        if (ints.any { it < 0 }) null else ints.map { it.toByte() }.toByteArray()
    } else {
        null
    }
}

fun ByteArray.hex(): String {
    val builder = StringBuilder()
    for (byte in this) {
        builder.append("%02x".format(Locale.US, byte.toInt() and 0xff))
    }
    return builder.toString()
}

fun Any.debug(message: String) {
    val tag = this::class.java.name.let {
        val index = it.lastIndexOf('.')
        if (index >= 0) it.substring(index + 1) else it
    }.replace('$', '.')
    Log.d(tag, message)
}
