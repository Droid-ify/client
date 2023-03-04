package com.looker.droidify.network

import com.looker.core.common.result.Result
import com.looker.droidify.utility.getProgress
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

	fun createCall(block: Request.Builder.() -> Unit): Call {
		val request = Request.Builder().apply(block).build()
		val onion = request.url.host.endsWith(".onion")
		val client = synchronized(clients) {
			val proxy = if (onion) onionProxy else proxy
			val clientConfiguration = ClientConfiguration(onion)
			clients[clientConfiguration] ?: run {
				val client = createClient(proxy)
				clients[clientConfiguration] = client
				client
			}
		}
		return client.newCall(request)
	}

	suspend fun downloadFile(
		url: String,
		target: File,
		lastModified: String,
		entityTag: String,
		authentication: String,
		callback: (read: Long, total: Long?) -> Unit
	): Result<RequestCode> = suspendCancellableCoroutine { cont ->
		val start = if (target.exists()) target.length().let { if (it > 0L) it else null } else null
		if (cont.isCompleted) return@suspendCancellableCoroutine
		val call = createCall {
			try {
				url(url)
				if (authentication.isNotEmpty()) addHeader("Authorization", authentication)
				if (entityTag.isNotEmpty()) addHeader("If-None-Match", entityTag)
				else if (lastModified.isNotEmpty()) addHeader("If-Modified-Since", lastModified)
				if (start != null) addHeader("Range", "bytes=$start-")
			} catch (e: IllegalArgumentException) {
				cont.resume(Result.Error(e))
			}
		}
		if (cont.isCompleted) return@suspendCancellableCoroutine
		call.enqueue(
			object : Callback {
				override fun onFailure(call: Call, e: IOException) {
					cont.resume(Result.Error(e))
				}

				override fun onResponse(call: Call, response: Response) {
					response.use { result ->
						if (response.code == HttpURLConnection.HTTP_NOT_MODIFIED) {
							if (cont.isCompleted) return
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
							if (cont.isCompleted) return
							val body = result.body
							val append = start != null && result.header("Content-Range") != null
							val progressStart = if (append && start != null) start else 0L
							val progressTotal =
								body.contentLength().let { if (it >= 0L) it else null }
									?.let { progressStart + it }
							val inputStream = body.byteStream().getProgress {
								callback(progressStart + it, progressTotal)
							}
							val outputStream = FileOutputStream(target, append)
							inputStream.use outerUse@{ input ->
								outputStream.use { output ->
									if (cont.isActive) {
										input.copyTo(output)
										output.fd.sync()
									}
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