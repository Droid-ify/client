package com.looker.downloader

import com.looker.downloader.model.DownloadItem
import com.looker.downloader.model.DownloadState
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object KtorDownloader : Downloader {

	private val httpClient = HttpClient(OkHttp) { expectSuccess = true }

	override suspend fun download(item: DownloadItem): Flow<DownloadState<Job>> =
		callbackFlow<DownloadState<Job>> {
			send(DownloadState.Pending())
			val job = launch {
				val httpResponse: HttpResponse = httpClient.get(item.url) {
					onDownload { bytesSent, contentLength ->
						send(
							DownloadState.Progress(
								total = contentLength,
								percent = bytesSent percentBy contentLength
							)
						)
					}
				}
				val responseBody: ByteArray = httpResponse.body()
				item.file.writeBytes(responseBody)
			}
			send(DownloadState.Success(job))
			awaitClose { println("Cancelled") }
		}.flowOn(Dispatchers.IO)

	override suspend fun cancelDownload(item: DownloadItem): Boolean = true
}

infix fun Long.percentBy(denominator: Long): Int = this.toInt().times(100) / denominator.toInt()