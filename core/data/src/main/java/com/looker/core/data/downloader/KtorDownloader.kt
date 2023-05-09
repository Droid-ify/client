package com.looker.core.data.downloader

import com.looker.core.data.downloader.header.HeadersBuilder
import com.looker.core.data.downloader.header.KtorHeaderBuilder
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.yield
import java.io.File

class KtorDownloader(private val client: HttpClient) : Downloader {
	override suspend fun headCall(
		url: String,
		headers: HeadersBuilder.() -> Unit
	): NetworkResponse {
		val status = client.head(url) {
			headers {
				KtorHeaderBuilder(this).headers()
			}
		}.status
		return if (status.isSuccess()) NetworkResponse.Success
		else NetworkResponse.Error(status.value)
	}

	override suspend fun downloadToFile(
		url: String,
		target: File,
		headers: HeadersBuilder.() -> Unit,
		block: ProgressListener?
	): NetworkResponse {
		val request = createRequest(url, target, headers, block)
		return try {
			client.prepareGet(request).execute { response ->
				val channel = response.bodyAsChannel()
				saveDownloadFile(channel, target)
				response.status.networkResponse()
			}
		} catch (e: Exception) {
			NetworkResponse.Error(-1, e)
		}
	}

	private fun createRequest(
		url: String,
		target: File,
		headers: HeadersBuilder.() -> Unit,
		block: ProgressListener?
	) = request {
		val cacheFileLength = if (target.exists()) target.length().takeIf { it >= 0 } else 0
		url(url)
		headers {
			val headerBuilder = KtorHeaderBuilder(this)
			with(headerBuilder) {
				headers()
				if (cacheFileLength != null) inRange(cacheFileLength)
			}
		}
		onDownload(block)
	}

	private suspend fun saveDownloadFile(channel: ByteReadChannel, target: File) {
		while (!channel.isClosedForRead) {
			val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
			while (!packet.isEmpty) {
				yield()
				val bytes = packet.readBytes()
				target.appendBytes(bytes)
			}
		}
	}

	private fun HttpStatusCode.networkResponse(): NetworkResponse =
		if (isSuccess()) NetworkResponse.Success
		else NetworkResponse.Error(value)
}