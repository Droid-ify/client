package com.looker.droidify.utility

import java.io.InputStream

class ProgressInputStream(
	private val inputStream: InputStream,
	private val callback: (Long) -> Unit,
) : InputStream() {
	private var count = 0L

	private inline fun <reified T : Number> notify(one: Boolean, read: () -> T): T {
		val result = read()
		count += if (one) 1L else result.toLong()
		callback(count)
		return result
	}

	override fun read(): Int = notify(true) { inputStream.read() }
	override fun read(b: ByteArray): Int = notify(false) { inputStream.read(b) }
	override fun read(b: ByteArray, off: Int, len: Int): Int =
		notify(false) { inputStream.read(b, off, len) }

	override fun skip(n: Long): Long = notify(false) { inputStream.skip(n) }

	override fun available(): Int {
		return inputStream.available()
	}

	override fun close() {
		inputStream.close()
		super.close()
	}
}
