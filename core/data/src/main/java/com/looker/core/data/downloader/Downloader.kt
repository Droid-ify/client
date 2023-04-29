package com.looker.core.data.downloader

import java.io.File

interface Downloader {

	suspend fun headCall(
		url: String,
		headers: Map<String, Any?> = emptyMap()
	): NetworkResponse

	suspend fun downloadToFile(
		url: String,
		target: File,
		headers: Map<String, Any?> = emptyMap(),
		block: ProgressListener
	): NetworkResponse

}

typealias ProgressListener = suspend (bytesReceived: Long, contentLength: Long) -> Unit