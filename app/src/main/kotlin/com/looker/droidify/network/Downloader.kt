package com.looker.droidify.network

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http.toHttpDateOrNull

typealias ProgressListener = suspend (bytesReceived: DataSize, contentLength: DataSize?) -> Unit

suspend fun OkHttpClient.head(
    url: String,
    block: Request.Builder.() -> Unit = {},
): NetworkResponse {
    val request = request(url) {
        head()
        block()
    }
    return try {
        newCall(request)
            .await()
            .use(Response::asNetworkResponse)
    } catch (e: Exception) {
        e.asNetworkError()
    }
}

suspend fun OkHttpClient.get(
    url: String,
    block: Request.Builder.() -> Unit = {},
    target: File,
    onProgress: ProgressListener? = null,
): NetworkResponse = withContext(Dispatchers.IO) {
    var output: FileOutputStream? = null
    try {
        output = FileOutputStream(target, true)
        val fileSize = target.length()
        val request = request(url) {
            if (fileSize > 0) range(fileSize)
            block()
        }

        val response = newCall(request).await()
        response.use { resp ->
            if (resp.isSuccessful) {
                val contentLength = resp.body.contentLength().takeIf { it > 0 }
                val totalLength = contentLength?.let { it + fileSize }

                resp.body.byteStream().use { inputStream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead: Int
                    var totalBytesRead = fileSize

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        onProgress?.invoke(
                            DataSize(totalBytesRead),
                            totalLength?.let { DataSize(it) }
                        )
                    }
                    output.flush()
                }
            }
            resp.asNetworkResponse()
        }
    } catch (e: Exception) {
        e.asNetworkError()
    } finally {
        withContext(NonCancellable) {
            runCatching { output?.close() }
        }
    }
}

inline fun request(url: String, block: Request.Builder.() -> Unit = {}): Request =
    Request.Builder()
        .url(url)
        .apply(block)
        .build()

private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) {
                continuation.resumeWithException(e)
            }
        }

        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }
    })
}

private fun Response.asNetworkResponse(): NetworkResponse {
    val statusCode = code
    return if (isSuccessful || statusCode == 304) {
        NetworkResponse.Success(
            statusCode = statusCode,
            lastModified = header("Last-Modified")?.toHttpDateOrNull(),
            etag = header("ETag"),
        )
    } else {
        NetworkResponse.Error.Http(statusCode)
    }
}

private fun Exception.asNetworkError(): NetworkResponse.Error = when (this) {
    is CancellationException -> throw this
    is SocketTimeoutException -> NetworkResponse.Error.SocketTimeout(this)
    is java.net.ConnectException -> NetworkResponse.Error.ConnectionTimeout(this)
    is IOException -> NetworkResponse.Error.IO(this)
    else -> NetworkResponse.Error.Unknown(this)
}
