package com.looker.droidify.network

import com.looker.droidify.utility.ProgressInputStream
import com.looker.droidify.utility.RxUtils
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.Cache
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

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

	class Result(val code: Int, val lastModified: String, val entityTag: String) {
		val success: Boolean
			get() = code == 200 || code == 206

		val isNotChanged: Boolean
			get() = code == 304
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

	fun download(
		url: String, target: File, lastModified: String, entityTag: String, authentication: String,
		callback: ((read: Long, total: Long?) -> Unit)?,
	): Single<Result> {
		val start = if (target.exists()) target.length().let { if (it > 0L) it else null } else null
		val request = Request.Builder().url(url)
			.apply {
				if (entityTag.isNotEmpty()) {
					addHeader("If-None-Match", entityTag)
				} else if (lastModified.isNotEmpty()) {
					addHeader("If-Modified-Since", lastModified)
				}
				if (start != null) {
					addHeader("Range", "bytes=$start-")
				}
			}

		return RxUtils
			.callSingle { createCall(request, authentication, null) }
			.subscribeOn(Schedulers.io())
			.flatMap { result ->
				RxUtils
					.managedSingle {
						result.use { it ->
							if (result.code == 304) {
								Result(it.code, lastModified, entityTag)
							} else {
								val body = it.body!!
								val append = start != null && it.header("Content-Range") != null
								val progressStart = if (append && start != null) start else 0L
								val progressTotal =
									body.contentLength().let { if (it >= 0L) it else null }
										?.let { progressStart + it }
								val inputStream = ProgressInputStream(body.byteStream()) {
									if (Thread.interrupted()) {
										throw InterruptedException()
									}
									callback?.invoke(progressStart + it, progressTotal)
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
								Result(
									it.code,
									it.header("Last-Modified").orEmpty(),
									it.header("ETag").orEmpty()
								)
							}
						}
					}
			}
	}
}
