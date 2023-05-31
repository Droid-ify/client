package com.looker.core.data.downloader

import com.looker.core.common.extension.exceptCancellation
import com.looker.core.common.extension.size
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

internal class KtorDownloader(private val client: HttpClient) : Downloader {
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
		return try {
			val request = createRequest(url, target.size, headers, block)
			client.prepareGet(request).execute { response ->
				val channel = response.bodyAsChannel()
				channel.saveToFile(target)
				response.status.toNetworkResponse()
			}
		} catch (e: Exception) {
			e.exceptCancellation()
			NetworkResponse.Error(-1, e)
		}
	}

	private fun createRequest(
		url: String,
		fileLength: Long?,
		headers: HeadersBuilder.() -> Unit,
		block: ProgressListener?
	) = request {
		url(url)
		headers {
			val headerBuilder = KtorHeaderBuilder(this)
			with(headerBuilder) {
				if (fileLength != null) inRange(fileLength)
				headers()
			}
		}
		onDownload(block)
	}

	private suspend fun ByteReadChannel.saveToFile(target: File) {
		while (!isClosedForRead) {
			val packet = readRemaining(DEFAULT_BUFFER_SIZE.toLong())
			while (!packet.isEmpty) {
				yield()
				val bytes = packet.readBytes()
				target.appendBytes(bytes)
			}
		}
	}

	private fun HttpStatusCode.toNetworkResponse(): NetworkResponse =
		if (isSuccess()) NetworkResponse.Success
		else NetworkResponse.Error(value)
}