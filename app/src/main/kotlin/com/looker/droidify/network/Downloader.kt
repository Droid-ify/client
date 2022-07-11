package com.looker.droidify.network

import com.looker.droidify.utility.ProgressInputStream
import com.looker.droidify.utility.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.IOException
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object Downloader {
	private data class ClientConfiguration(val cache: Cache?, val onion: Boolean)

	private val clients = mutableMapOf<ClientConfiguration, OkHttpClient>()
	private val onionProxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 9050))

	var proxy: Proxy? = null
		set(value) {
			if (field != value) {
				synchronized(clients) {
					field = value
					clients.keys.removeAll { !it.onion }
				}
			}
		}

	private fun createClient(proxy: Proxy?, cache: Cache?): OkHttpClient {
		return OkHttpClient.Builder()
			.fastFallback(true)
			.connectTimeout(30L, TimeUnit.SECONDS)
			.readTimeout(15L, TimeUnit.SECONDS)
			.writeTimeout(15L, TimeUnit.SECONDS)
			.proxy(proxy).cache(cache).build()
	}

	class RequestCode(val code: Int, val lastModified: String, val entityTag: String) {
		val success: Boolean
			get() = code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_PARTIAL

		val isNotChanged: Boolean
			get() = code == HttpURLConnection.HTTP_NOT_MODIFIED
	}

	fun createCall(request: Request.Builder, authentication: String, cache: Cache?): Call {
		val oldRequest = request.build()
		val newRequest = if (authentication.isNotEmpty()) {
			request.addHeader("Authorization", authentication).build()
		} else {
			request.build()
		}
		val onion = oldRequest.url.host.endsWith(".onion")
		val client = synchronized(clients) {
			val proxy = if (onion) onionProxy else proxy
			val clientConfiguration = ClientConfiguration(cache, onion)
			clients[clientConfiguration] ?: run {
				val client = createClient(proxy, cache)
				clients[clientConfiguration] = client
				client
			}
		}
		return client.newCall(newRequest)
	}

	suspend fun createCallIO(
		request: Request.Builder,
		authentication: String,
		cache: Cache?
	): Call = withContext(Dispatchers.IO) { createCall(request, authentication, cache) }

	suspend inline fun downloadFile(
		url: String, target: File, lastModified: String, entityTag: String, authentication: String,
		crossinline callback: (read: Long, total: Long?) -> Unit
	): Result<RequestCode> {
		val start = if (target.exists()) target.length().let { if (it > 0L) it else null } else null
		val request = Request.Builder().url(url)
			.apply {
				if (entityTag.isNotEmpty()) addHeader("If-None-Match", entityTag)
				else if (lastModified.isNotEmpty()) addHeader("If-Modified-Since", lastModified)
				if (start != null) addHeader("Range", "bytes=$start-")
			}
		val call = createCallIO(request, authentication, null)
		return suspendCancellableCoroutine { cont ->
			call.enqueue(
				object : Callback {
					override fun onFailure(call: Call, e: IOException) {
						cont.resume(Result.Error(e))
					}

					override fun onResponse(call: Call, response: Response) {
						response.use { result ->
							if (response.code == 304) {
								cont.resume(
									Result.Success(
										RequestCode(
											result.code,
											lastModified,
											entityTag
										)
									)
								)
							} else {
								val body = result.body
								val append = start != null && result.header("Content-Range") != null
								val progressStart = if (append && start != null) start else 0L
								val progressTotal =
									body.contentLength().let { if (it >= 0L) it else null }
										?.let { progressStart + it }
								val inputStream = ProgressInputStream(body.byteStream()) {
									callback(progressStart + it, progressTotal)
								}
								inputStream.use { input ->
									val outputStream = if (append) FileOutputStream(
										target,
										true
									) else FileOutputStream(target)
									outputStream.use { output ->
										input.copyTo(output)
										output.fd.sync()
									}
								}
								cont.resume(
									Result.Success(
										RequestCode(
											result.code,
											result.header("Last-Modified").orEmpty(),
											result.header("ETag").orEmpty()
										)
									)
								)
							}
						}
					}
				}
			)
			cont.invokeOnCancellation { call.cancel() }
		}
	}
}