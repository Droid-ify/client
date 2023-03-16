package com.looker.downloader

import android.content.Context
import com.looker.core.common.extension.percentBy
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.downloader.model.DownloadItem
import com.looker.downloader.model.DownloadItemState
import com.looker.downloader.model.DownloadLocation
import com.looker.downloader.model.DownloadState
import com.looker.downloader.model.HeaderInfo
import com.looker.downloader.model.statesTo
import com.looker.downloader.model.toItem
import com.looker.downloader.model.toLocation
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
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
import io.ktor.http.ifModifiedSince
import io.ktor.http.ifNoneMatch
import io.ktor.http.lastModified
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.io.IOException

class Downloader(
	private val context: Context,
	private val repository: UserPreferencesRepository
) {

	// FIXME: Placeholder
	private val client: HttpClient = HttpClient(OkHttp)

	private val downloadQueueState = MutableStateFlow(emptySet<String>())
	private val downloadState = MutableStateFlow(DownloadItemState.EMPTY)

	private val locations = Channel<DownloadLocation>()
	private val downloads = Channel<DownloadItem>()

	// This returns the url
	private val contents = Channel<String>(1)

	suspend operator fun invoke(workers: Int = 3) = coroutineScope {
		repeat(workers) { worker(locations, contents, downloadState) }
		downloader(downloads, contents, locations, downloadQueueState)
	}

	private fun CoroutineScope.downloader(
		downloads: ReceiveChannel<DownloadItem>,
		contents: ReceiveChannel<String>,
		locations: SendChannel<DownloadLocation>,
		queueState: MutableStateFlow<Set<String>>
	) = launch {
		val requested = mutableSetOf<String>()
		while (true) {
			select {
				contents.onReceive { url ->
					requested.remove(url)
					queueState.emit(requested)
				}
				downloads.onReceive { item ->
					val location = item.toLocation(context)
					val isAdded = requested.add(item.url)
					if (isAdded) {
						queueState.emit(requested)
						locations.send(location)
					}
				}
			}
		}
	}

	private fun CoroutineScope.worker(
		locations: ReceiveChannel<DownloadLocation>,
		contents: SendChannel<String>,
		downloadState: MutableStateFlow<DownloadItemState>
	) = launch {
		locations.consumeEach { loc ->
			mockDownload(loc, downloadState)
			contents.send(loc.url)
		}
	}

	private suspend fun mockDownload(
		location: DownloadLocation,
		state: MutableStateFlow<DownloadItemState>
	) {
		state.emit(location.toItem() statesTo DownloadState.Connecting)
		delay(1000)
		state.emit(location.toItem() statesTo DownloadState.Progress(0, 100))
		delay(1000)
		state.emit(location.toItem() statesTo DownloadState.Progress(50, 100))
		delay(1000)
		state.emit(location.toItem() statesTo DownloadState.Progress(100, 100))
		delay(1000)
		state.emit(location.toItem() statesTo DownloadState.Success(location.headerInfo))
	}

	private suspend fun download(item: DownloadLocation): Flow<DownloadState> =
		callbackFlow {
			val partialFileLength = item.file.length()
			val request = HttpRequestBuilder().apply {
				url(item.url)
				header(HttpHeaders.Range, item.headerInfo.authentication)
				header(HttpHeaders.Range, "bytes=${partialFileLength}-")
				if (item.headerInfo.etag != null) {
					ifNoneMatch(item.headerInfo.etag)
				} else if (item.headerInfo.lastModified != null) {
					ifModifiedSince(item.headerInfo.lastModified)
				}
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
					authentication = item.headerInfo.authentication
				)
				send(DownloadState.Success(header))
				close()
			}
			awaitClose { println("Cancelled") }
		}.onStart {
			emit(DownloadState.Connecting)
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