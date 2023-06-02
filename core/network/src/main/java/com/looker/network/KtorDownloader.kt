package com.looker.network

import com.looker.core.common.extension.size
import com.looker.network.header.HeadersBuilder
import com.looker.network.header.KtorHeadersBuilder
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
import javax.inject.Inject

class KtorDownloader @Inject constructor(private val client: HttpClient) : Downloader {

	override suspend fun headCall(
		url: String,
		headers: HeadersBuilder.() -> Unit
	): NetworkResponse {
		val status = client.head(url) {
			headers {
				KtorHeadersBuilder(this).headers()
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
			val headerBuilder = KtorHeadersBuilder(this)
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