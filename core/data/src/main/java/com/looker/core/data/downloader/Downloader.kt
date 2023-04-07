package com.looker.core.data.downloader

import java.io.File

interface Downloader {

	suspend fun headCall(
		url: String,
		headers: Map<String, Any?> = emptyMap()
	): Boolean

	suspend fun downloadToFile(
		url: String,
		target: File,
		headers: Map<String, Any?> = emptyMap(),
		block: ProgressListener
	): Boolean

}

typealias ProgressListener = suspend (bytesSentTotal: Long, contentLength: Long) -> Unit