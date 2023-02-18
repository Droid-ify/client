package com.looker.core.common

import android.util.Log
import java.util.*

fun <T : CharSequence> T.nullIfEmpty(): T? {
	return if (isNullOrBlank()) null else this
}

private val sizeFormats = listOf("%.0f B", "%.0f kB", "%.1f MB", "%.2f GB")

fun Long.formatSize(): String {
	val (size, index) = generateSequence(Pair(this.toFloat(), 0)) { (size, index) ->
		if (size >= 1024f)
			Pair(size / 1024f, index + 1) else null
	}.take(sizeFormats.size).last()
	return sizeFormats[index].format(Locale.US, size)
}

fun ByteArray.hex(): String = buildString {
	this@hex.forEach { byte ->
		append("%02x".format(Locale.US, byte.toInt() and 0xff))
	}
}

fun Any.debug(message: String) {
	val tag = this::class.java.name.let {
		val index = it.lastIndexOf('.')
		if (index >= 0) it.substring(index + 1) else it
	}.replace('$', '.') + "DEBUG"
	Log.d(tag, message)
}
