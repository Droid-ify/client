package com.looker.network

import com.looker.network.header.HeadersBuilder
import java.io.File
import java.net.Proxy

interface Downloader {

	fun setProxy(proxy: Proxy)

	suspend fun headCall(
		url: String,
		headers: HeadersBuilder.() -> Unit = {}
	): NetworkResponse

	suspend fun downloadToFile(
		url: String,
		target: File,
		headers: HeadersBuilder.() -> Unit = {},
		block: ProgressListener? = null
	): NetworkResponse

}

typealias ProgressListener = suspend (bytesReceived: Long, contentLength: Long) -> Unit