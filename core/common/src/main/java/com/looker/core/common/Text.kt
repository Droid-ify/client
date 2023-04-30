package com.looker.core.common

import android.util.Log
import java.util.*

fun <T : CharSequence> T.nullIfEmpty(): T? {
	return if (isNullOrBlank()) null else this
}

fun String.stripBetween(prefix: String, suffix: String = prefix): String {
	val firstHyphenIndex = this.indexOf(prefix)
	val lastHyphenIndex = this.lastIndexOf(suffix)
	return if (firstHyphenIndex != -1 && lastHyphenIndex != -1 && firstHyphenIndex != lastHyphenIndex) {
		this.substring(0, firstHyphenIndex + 1) + this.substring(lastHyphenIndex + 1)
	} else {
		this
	}
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

fun Any.log(message: String) {
	this::class.java.simpleName
	val tag = this::class.java.simpleName + ".DEBUG"
	Log.d(tag, message)
}
