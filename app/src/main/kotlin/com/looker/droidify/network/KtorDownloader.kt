package com.looker.droidify.network

import com.looker.droidify.BuildConfig
import com.looker.droidify.network.header.HeadersBuilder
import com.looker.droidify.network.header.KtorHeadersBuilder
import com.looker.droidify.network.validation.FileValidator
import com.looker.droidify.network.validation.ValidationException
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
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.Proxy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

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
        headers: HeadersBuilder.() -> Unit,
    ): NetworkResponse {
        val headRequest = request(url, headers = headers)
        return client.head(headRequest).asNetworkResponse()
    }

    override suspend fun downloadToFile(
        url: String,
        target: File,
        validator: FileValidator?,
        headers: HeadersBuilder.() -> Unit,
        block: ProgressListener?,
    ): NetworkResponse = withContext(dispatcher) {
        val output = FileOutputStream(target, true)
        try {
            val fileSize = target.length()
            val request = request(
                url = url,
                fileSize = fileSize,
                block = block,
            ) {
                inRange(fileSize)
                headers()
            }
            client.prepareGet(request).execute { response ->
                val networkResponse = response.asNetworkResponse()
                if (networkResponse !is NetworkResponse.Success) {
                    return@execute networkResponse
                }
                response.bodyAsChannel().copyTo(output)
                output.flush()
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetworkResponse.Error.Unknown(e)
        } finally {
            withContext(NonCancellable) {
                output.close()
                output.flush()
            }
        }
    }

    private fun client(
        engine: HttpClientEngine = OkHttp.create(),
    ) = HttpClient(engine) {
        userAgentConfig()
        timeoutConfig()
    }

    private fun request(
        url: String,
        fileSize: Long = 0L,
        block: ProgressListener? = null,
        headers: HeadersBuilder.() -> Unit,
    ) = request {
        url(url)
        headers { KtorHeadersBuilder(this).headers() }
        if (block != null) {
            onDownload { read, total ->
                block(
                    DataSize(read + fileSize),
                    total?.let { DataSize(total + fileSize) },
                )
            }
        }
    }
}

private const val CONNECTION_TIMEOUT = 30_000L
private const val SOCKET_TIMEOUT = 15_000L
private const val USER_AGENT = "Droid-ify/${BuildConfig.VERSION_NAME}-${BuildConfig.BUILD_TYPE}"

private fun HttpClientConfig<*>.userAgentConfig() = install(UserAgent) {
    agent = USER_AGENT
}

private fun HttpClientConfig<*>.timeoutConfig() = install(HttpTimeout) {
    connectTimeoutMillis = CONNECTION_TIMEOUT
    socketTimeoutMillis = SOCKET_TIMEOUT
}

private fun HttpResponse.asNetworkResponse(): NetworkResponse =
    if (status.isSuccess() || status == HttpStatusCode.NotModified) {
        NetworkResponse.Success(status.value, lastModified(), etag())
    } else {
        NetworkResponse.Error.Http(status.value)
    }
