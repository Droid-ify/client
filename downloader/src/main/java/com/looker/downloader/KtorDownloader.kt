package com.looker.downloader

import android.util.Log
import com.looker.downloader.model.DownloadItem
import com.looker.downloader.model.DownloadState
import com.looker.downloader.model.HeaderInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.etag
import io.ktor.http.lastModified
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class KtorDownloader(private val httpClient: HttpClient) : Downloader {

	companion object {
		private const val TAG = "KtorDownloader"
		private val HTTP_DATE_FORMAT: SimpleDateFormat
			get() = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
				timeZone = TimeZone.getTimeZone("GMT")
			}
	}

	override suspend fun download(item: DownloadItem): Flow<DownloadState> =
		callbackFlow {
			send(DownloadState.Pending)
			val httpRequest = HttpRequestBuilder().apply {
				url(item.url)
				header(HttpHeaders.Authorization, item.headerInfo.authorization)
				onDownload { bytesSent, contentLength ->
					Log.i(TAG, "download: bytesSent: $bytesSent, contentLength: $contentLength")
					send(
						DownloadState.Progress(
							total = contentLength,
							current = bytesSent,
							percent = bytesSent percentBy contentLength
						)
					)
				}
			}
			val httpResponse: HttpResponse? = try {
				httpClient.get(httpRequest)
			} catch (e: RedirectResponseException) {
				send(DownloadState.Error.HttpError(e.response.status.value, e))
				null
			} catch (e: ClientRequestException) {
				send(DownloadState.Error.HttpError(e.response.status.value, e))
				null
			} catch (e: ServerResponseException) {
				send(DownloadState.Error.HttpError(e.response.status.value, e))
				null
			} catch (e: Exception) {
				send(DownloadState.Error.UnknownError)
				null
			}
			val responseBody: ByteArray? = httpResponse?.body()
			try {
				responseBody?.let { item.file.writeBytes(it) }
			} catch (e: IOException) {
				send(DownloadState.Error.IOError(e))
			}
			val headerInfo = HeaderInfo(
				eTag = httpResponse?.etag(),
				lastModified = httpResponse?.lastModified()?.let { HTTP_DATE_FORMAT.format(it) }
			)
			send(DownloadState.Success(headerInfo))
			awaitClose { println("Cancelled") }
		}.flowOn(Dispatchers.IO)

}

infix fun Long.percentBy(denominator: Long): Int = this.toInt().times(100) / denominator.toInt()