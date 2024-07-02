package com.looker.network

import com.looker.core.common.DataSize
import com.looker.core.common.extension.exceptCancellation
import com.looker.core.common.extension.size
import com.looker.core.common.signature.FileValidator
import com.looker.core.common.signature.ValidationException
import com.looker.network.Downloader.Companion.CONNECTION_TIMEOUT
import com.looker.network.Downloader.Companion.SOCKET_TIMEOUT
import com.looker.network.header.HeadersBuilder
import com.looker.network.header.KtorHeadersBuilder
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
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
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.Proxy

internal class KtorDownloader : Downloader {

    private var client = client(null)
        set(newClient) {
            field.close()
            field = newClient
        }

    override fun setProxy(proxy: Proxy) {
        client = client(proxy)
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
    ): NetworkResponse = withContext(Dispatchers.IO) {
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
                response.bodyAsChannel() saveTo target
                validator?.validate(target)
                response.asNetworkResponse()
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
            e.exceptCancellation()
            NetworkResponse.Error.Unknown(e)
        }
    }

    private companion object {

        fun client(proxy: Proxy?): HttpClient {
            return HttpClient(OkHttp) {
                userAgentConfig()
                timeoutConfig()
                engine { this.proxy = proxy }
            }
        }

        const val USER_AGENT =
            "Droid-ify, Mode: ${BuildConfig.BUILD_TYPE}, Version: ${BuildConfig.VERSION_NAME}"

        fun HttpClientConfig<OkHttpConfig>.userAgentConfig() = install(UserAgent) {
            agent = USER_AGENT
        }

        fun HttpClientConfig<OkHttpConfig>.timeoutConfig() = install(HttpTimeout) {
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
