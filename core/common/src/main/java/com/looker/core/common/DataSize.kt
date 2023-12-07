package com.looker.core.common

import java.util.Locale

@JvmInline
value class DataSize(val value: Long) {

    companion object {
        private const val BYTE_SIZE = 1024L
        private val sizeFormats = listOf("%.0f B", "%.0f kB", "%.1f MB", "%.2f GB")
    }

    override fun toString(): String {
        val (size, index) = generateSequence(Pair(value.toFloat(), 0)) { (size, index) ->
            if (size >= BYTE_SIZE) {
                Pair(size / BYTE_SIZE, index + 1)
            } else {
                null
            }
        }.take(sizeFormats.size).last()
        return sizeFormats[index].format(Locale.US, size)
    }
}
