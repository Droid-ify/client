package com.looker.droidify.network

import com.looker.core.common.result.Result
import com.looker.droidify.utility.ProgressInputStream
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object Downloader {
	@JvmInline
	private value class ClientConfiguration(val onion: Boolean)

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

	private fun createClient(proxy: Proxy?): OkHttpClient {
		return OkHttpClient.Builder()
			.fastFallback(true)
			.connectTimeout(30L, TimeUnit.SECONDS)
			.readTimeout(15L, TimeUnit.SECONDS)
			.writeTimeout(15L, TimeUnit.SECONDS)
			.proxy(proxy).build()
	}

	class RequestCode(val code: Int, val lastModified: String, val entityTag: String) {
		val success: Boolean
			get() = code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_PARTIAL

		val isNotChanged: Boolean
			get() = code == HttpURLConnection.HTTP_NOT_MODIFIED
	}

	fun createCall(request: Request.Builder, authentication: String): Call {
		val oldRequest = request.build()
		val newRequest = if (authentication.isNotEmpty()) {
			request.addHeader("Authorization", authentication).build()
		} else {
			request.build()
		}
		val onion = oldRequest.url.host.endsWith(".onion")
		val client = synchronized(clients) {
			val proxy = if (onion) onionProxy else proxy
			val clientConfiguration = ClientConfiguration(onion)
			clients[clientConfiguration] ?: run {
				val client = createClient(proxy)
				clients[clientConfiguration] = client
				client
			}
		}
		return client.newCall(newRequest)
	}

	suspend inline fun downloadFile(
		url: String, target: File, lastModified: String, entityTag: String, authentication: String,
		crossinline callback: (read: Long, total: Long?) -> Unit
	): Result<RequestCode> = suspendCancellableCoroutine { cont ->
		val start = if (target.exists()) target.length().let { if (it > 0L) it else null } else null
		val request = try {
			Request.Builder().url(url)
		} catch (e: IllegalArgumentException) {
			e.printStackTrace()
			cont.resume(Result.Error(e))
			null
		}?.apply {
			if (entityTag.isNotEmpty()) addHeader("If-None-Match", entityTag)
			else if (lastModified.isNotEmpty()) addHeader("If-Modified-Since", lastModified)
			if (start != null) addHeader("Range", "bytes=$start-")
		}
		val call = request?.let { createCall(it, authentication) }
		call?.enqueue(
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
		cont.invokeOnCancellation { call?.cancel() }
	}
}