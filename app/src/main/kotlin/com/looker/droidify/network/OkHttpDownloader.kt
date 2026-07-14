package com.looker.droidify.network

import com.looker.droidify.network.header.inRange
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSink
import okio.BufferedSource
import okio.appendingSink
import okio.buffer

internal class OkHttpDownloader(
    private val client: OkHttpClient,
    private val dispatcher: CoroutineDispatcher,
) : Downloader {

    override suspend fun headCall(
        url: String,
        headers: Headers.Builder.() -> Unit,
    ): NetworkResponse = withContext(dispatcher) {
        val headRequest = request(url, headers)
            .head()
            .build()
        client.newCall(headRequest).execute().use { it.asNetworkResponse() }
    }

    override suspend fun downloadToFile(
        url: String,
        target: File,
        headers: Headers.Builder.() -> Unit,
        block: ProgressListener?,
    ): NetworkResponse = withContext(dispatcher) {
        try {
            val fileSize = target.length()
            val request = request(url) {
                if (fileSize > 0) inRange(fileSize)
                headers()
            }.build()
            client.newCall(request).execute().use { response ->
                val networkResponse = response.asNetworkResponse()
                if (networkResponse !is NetworkResponse.Success) return@use networkResponse

                target.appendingSink().buffer().use { sink ->
                    response.body.source().copyTo(
                        sink = sink,
                        alreadyRead = fileSize,
                        contentLength = response.body.contentLength().takeIf { it >= 0 },
                        block = block,
                    )
                }
                networkResponse
            }
        } catch (e: SocketTimeoutException) {
            NetworkResponse.Error.SocketTimeout(e)
        } catch (e: ConnectException) {
            NetworkResponse.Error.ConnectionTimeout(e)
        } catch (e: IOException) {
            NetworkResponse.Error.IO(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResponse.Error.Unknown(e)
        }
    }

    private suspend fun BufferedSource.copyTo(
        sink: BufferedSink,
        alreadyRead: Long,
        contentLength: Long?,
        block: ProgressListener?,
    ) = withContext(dispatcher) {
        var totalRead = 0L
        while (true) {
            val read = read(sink.buffer, DEFAULT_BUFFER_SIZE.toLong())
            if (read == -1L) break
            sink.emitCompleteSegments()
            totalRead += read
            block?.invoke(
                DataSize(totalRead + alreadyRead),
                contentLength?.let { DataSize(it + alreadyRead) },
            )
        }
    }

    private fun request(
        url: String,
        headers: Headers.Builder.() -> Unit,
    ): Request.Builder = Request.Builder()
        .url(url)
        .headers(Headers.Builder().apply(headers).build())
}

private fun Response.asNetworkResponse(): NetworkResponse =
    if (isSuccessful || code == HTTP_NOT_MODIFIED) {
        NetworkResponse.Success(code, headers.getDate("Last-Modified"), header("ETag"))
    } else {
        NetworkResponse.Error.Http(code)
    }
