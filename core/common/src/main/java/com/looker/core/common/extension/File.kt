package com.looker.core.common.extension

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

val File.size: Long?
	get() = if (exists()) length().takeIf { it > 0L } else null

suspend fun File.readFrom(input: InputStream) = withContext(Dispatchers.IO) {
	val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
	var bytesRead = input.read(buffer)
	val output = FileOutputStream(this@readFrom)
	while (bytesRead != -1) {
		ensureActive()
		output.write(buffer, 0, bytesRead)
		yield()
		bytesRead = input.read(buffer)
	}
	output.flush()
	output.close()
}