package com.looker.downloader

import com.looker.core.common.percentBy
import com.looker.downloader.model.DownloadItem
import com.looker.downloader.model.DownloadState
import com.looker.downloader.model.HeaderInfo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.etag
import io.ktor.http.lastModified
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import java.io.IOException

class KtorDownloader(private val client: HttpClient) : Downloader {

	override suspend fun download(item: DownloadItem): Flow<DownloadState> =
		callbackFlow {
			val partialFileLength = item.file.length()
			val request = HttpRequestBuilder().apply {
				url(item.url)
				header(HttpHeaders.Authorization, item.headerInfo.authorization)
				header(HttpHeaders.Range, "bytes=${partialFileLength}-")
				onDownload { bytesSentTotal, contentLength ->
					send(
						DownloadState.Progress(
							total = contentLength,
							current = bytesSentTotal,
							percent = bytesSentTotal percentBy contentLength
						)
					)
				}
			}
			client.prepareGet(request).execute { response ->
				val channel = response.bodyAsChannel()
				while (!channel.isClosedForRead) {
					val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
					while (!packet.isEmpty) {
						val bytes = packet.readBytes()
						item.file.appendBytes(bytes)
					}
				}
				val header = HeaderInfo(
					etag = response.etag(),
					lastModified = response.lastModified(),
					authorization = item.headerInfo.authorization
				)
				send(DownloadState.Success(header))
			}
			awaitClose { println("Cancelled") }
		}.onStart {
			emit(DownloadState.Pending)
		}.catch { error ->
			when (error) {
				is RedirectResponseException -> emit(DownloadState.Error.RedirectError(error))
				is ClientRequestException -> emit(DownloadState.Error.ClientError(error))
				is ServerResponseException -> emit(DownloadState.Error.ServerError(error))
				is IOException -> emit(DownloadState.Error.IOError(error))
				is Exception -> emit(DownloadState.Error.UnknownError)
			}
		}.flowOn(Dispatchers.IO)

}