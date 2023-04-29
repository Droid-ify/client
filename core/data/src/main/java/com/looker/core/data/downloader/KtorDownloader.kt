package com.looker.core.data.downloader

import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.yield
import java.io.File

class KtorDownloader(private val client: HttpClient) : Downloader {
	override suspend fun headCall(
		url: String,
		headers: Map<String, Any?>
	): NetworkResponse {
		val status = client.head(url) {
			headers {
				headers.forEach { entry ->
					entry.value?.let { append(entry.key, it.toString()) }
				}
			}
		}.status
		return if (status.isSuccess()) NetworkResponse.Success
		else NetworkResponse.Error(status.value)
	}

	override suspend fun downloadToFile(
		url: String,
		target: File,
		headers: Map<String, Any?>,
		block: ProgressListener
	): NetworkResponse {
		val cacheFileLength = if (target.exists()) target.length().takeIf { it >= 0 } else 0
		val request = request {
			url(url)
			headers {
				headers.forEach { entry ->
					entry.value?.let { append(entry.key, it.toString()) }
					cacheFileLength?.let { append(HttpHeaders.Range, "bytes=${it}-") }
				}
			}
			onDownload(block)
		}
		return try {
			client.prepareGet(request).execute { response ->
				val channel = response.bodyAsChannel()
				while (!channel.isClosedForRead) {
					val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
					while (!packet.isEmpty) {
						yield()
						val bytes = packet.readBytes()
						target.appendBytes(bytes)
					}
				}
				response.status.isSuccess()
				if (response.status.isSuccess()) NetworkResponse.Success
				else NetworkResponse.Error(response.status.value)
			}
		} catch (e: Exception) {
			NetworkResponse.Error(-1, e)
		}
	}
}