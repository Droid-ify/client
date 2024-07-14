package com.looker.network

import com.looker.network.Downloader.Companion.CONNECTION_TIMEOUT
import com.looker.network.Downloader.Companion.SOCKET_TIMEOUT
import com.looker.network.Downloader.Companion.USER_AGENT
import com.looker.network.header.HeadersBuilder
import com.looker.network.header.KtorHeadersBuilder
import com.looker.network.validation.FileValidator
import com.looker.network.validation.ValidationException
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.head
import io.ktor.client.request.headers
import io.ktor.client.request.prepareGet
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.etag
import io.ktor.http.isSuccess
import io.ktor.http.lastModified
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.Proxy

internal class KtorDownloader(
    httpClientEngine: HttpClientEngine,
    private val dispatcher: CoroutineDispatcher,
) : Downloader {

    private var client = client(httpClientEngine)
        set(newClient) {
            field.close()
            field = newClient
        }

    override fun setProxy(proxy: Proxy) {
        client = client(OkHttp.create { this.proxy = proxy })
    }

    override suspend fun headCall(
        url: String,
        headers: HeadersBuilder.() -> Unit
    ): NetworkResponse {
        val headRequest = createRequest(
            url = url,
            headers = headers
        )
        return client.head(headRequest).asNetworkResponse()
    }

    override suspend fun downloadToFile(
        url: String,
        target: File,
        validator: FileValidator?,
        headers: HeadersBuilder.() -> Unit,
        block: ProgressListener?
    ): NetworkResponse = withContext(dispatcher) {
        try {
            val request = createRequest(
                url = url,
                headers = {
                    inRange(target.size)
                    headers()
                },
                fileSize = target.size,
                block = block
            )
            client.prepareGet(request).execute { response ->
                val networkResponse = response.asNetworkResponse()
                if (networkResponse !is NetworkResponse.Success) {
                    return@execute networkResponse
                }
                response.bodyAsChannel() saveTo target
                validator?.validate(target)
                networkResponse
            }
        } catch (e: SocketTimeoutException) {
            NetworkResponse.Error.SocketTimeout(e)
        } catch (e: ConnectTimeoutException) {
            NetworkResponse.Error.ConnectionTimeout(e)
        } catch (e: IOException) {
            NetworkResponse.Error.IO(e)
        } catch (e: ValidationException) {
            target.delete()
            NetworkResponse.Error.Validation(e)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            NetworkResponse.Error.Unknown(e)
        }
    }

    private companion object {

        fun client(
            engine: HttpClientEngine = OkHttp.create()
        ): HttpClient {
            return HttpClient(engine) {
                userAgentConfig()
                timeoutConfig()
            }
        }

        fun HttpClientConfig<*>.userAgentConfig() = install(UserAgent) {
            agent = USER_AGENT
        }

        fun HttpClientConfig<*>.timeoutConfig() = install(HttpTimeout) {
            connectTimeoutMillis = CONNECTION_TIMEOUT
            socketTimeoutMillis = SOCKET_TIMEOUT
        }

        fun createRequest(
            url: String,
            headers: HeadersBuilder.() -> Unit,
            fileSize: Long? = null,
            block: ProgressListener? = null
        ) = request {
            url(url)
            headers {
                KtorHeadersBuilder(this).headers()
            }
            onDownload { read, total ->
                if (block != null) {
                    block(DataSize(read + (fileSize ?: 0L)), DataSize(total + (fileSize ?: 0L)))
                }
            }
        }

        suspend infix fun ByteReadChannel.saveTo(target: File) =
            withContext(Dispatchers.IO) {
                while (!isClosedForRead && isActive) {
                    val packet = readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    packet.appendTo(target)
                }
            }

        suspend fun ByteReadPacket.appendTo(file: File) =
            withContext(Dispatchers.IO) {
                while (!isEmpty && isActive) {
                    val bytes = readBytes()
                    file.appendBytes(bytes)
                }
            }

        fun HttpResponse.asNetworkResponse(): NetworkResponse =
            if (status.isSuccess() || status == HttpStatusCode.NotModified) {
                NetworkResponse.Success(status.value, lastModified(), etag())
            } else {
                NetworkResponse.Error.Http(status.value)
            }
    }
}
