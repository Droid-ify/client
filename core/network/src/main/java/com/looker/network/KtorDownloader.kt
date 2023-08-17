package com.looker.network

import com.looker.core.common.extension.exceptCancellation
import com.looker.core.common.extension.size
import com.looker.network.header.HeadersBuilder
import com.looker.network.header.KtorHeadersBuilder
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.*
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.yield
import java.io.File
import java.util.Date
import javax.inject.Inject

class KtorDownloader @Inject constructor(private val client: HttpClient) : Downloader {

	override suspend fun headCall(
		url: String,
		headers: HeadersBuilder.() -> Unit
	): NetworkResponse {
		val headRequest = createRequest(
			url = url,
			headers = headers,
			block = null
		)
		val status = client.head(headRequest).status
		return status.toNetworkResponse()
	}

	override suspend fun downloadToFile(
		url: String,
		target: File,
		headers: HeadersBuilder.() -> Unit,
		block: ProgressListener?
	): NetworkResponse {
		return try {
			val request = createRequest(
				url = url,
				headers = {
					target.size?.let { inRange(it) }
					headers()
				},
				block = block
			)
			client.prepareGet(request).execute { response ->
				response.bodyAsChannel() saveTo target
				response.status.toNetworkResponse(
					lastModified = response.lastModified(),
					etag = response.etag()
				)
			}
		} catch (e: Exception) {
			e.exceptCancellation()
			NetworkResponse.Error(-1, e)
		}
	}

	private fun createRequest(
		url: String,
		headers: HeadersBuilder.() -> Unit,
		block: ProgressListener?
	) = request {
		url(url)
		headers {
			KtorHeadersBuilder(this).headers()
		}
		onDownload(block)
	}

	private suspend infix fun ByteReadChannel.saveTo(target: File) {
		while (!isClosedForRead) {
			yield()
			val packet = readRemaining(DEFAULT_BUFFER_SIZE.toLong())
			packet.appendTo(target)
		}
	}

	private suspend fun ByteReadPacket.appendTo(file: File) {
		while (!isEmpty) {
			yield()
			val bytes = readBytes()
			file.appendBytes(bytes)
		}
	}

	private fun HttpStatusCode.toNetworkResponse(
		lastModified: Date? = null,
		etag: String? = null
	): NetworkResponse =
		if (isSuccess() || this == HttpStatusCode.NotModified) NetworkResponse.Success(value, lastModified, etag)
		else NetworkResponse.Error(value)

}