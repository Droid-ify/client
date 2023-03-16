package com.looker.core.common.extension

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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