package com.looker.downloader.model

import android.util.Log
import io.ktor.http.*

data class Mirror(
	val baseUrl: String,
	val location: String? = null
) {
	val url: Url by lazy {
		try {
			URLBuilder(baseUrl.trimEnd('/')).build()
		} catch (e: URLParserException) {
			Log.w("Mirrors", "Invalid url: $baseUrl")
			URLBuilder("http://127.0.0.1:64335").build()
		} catch (e: IllegalArgumentException) {
			Log.w("Mirrors", "Invalid url: $baseUrl")
			URLBuilder("http://127.0.0.1:64335").build()
		}
	}

	fun getUrl(path: String): Url {
		return URLBuilder(url).appendPathSegments(path).build()
	}

	fun isOnion(): Boolean = url.isOnion()

	fun isLocal(): Boolean = url.isLocal()

	fun isHttp(): Boolean = url.protocol.name.startsWith("http")

	companion object {
		@JvmStatic
		fun fromStrings(list: List<String>): List<Mirror> = list.map { Mirror(it) }
	}
}

internal fun Mirror?.isLocal(): Boolean = this?.isLocal() == true

internal fun Url.isOnion(): Boolean = host.endsWith(".onion")

/**
 * Returns true when no proxy should be used for connecting to this [Url].
 */
internal fun Url.isLocal(): Boolean {
	if (!host.matches(Regex("[0-9.]{7,15}"))) return false
	if (host.startsWith("172.")) {
		val second = host.substring(4..6)
		if (!second.endsWith('.')) return false
		val num = second.trimEnd('.').toIntOrNull() ?: return false
		return num in 16..31
	}
	return host.startsWith("169.254.") ||
			host.startsWith("10.") ||
			host.startsWith("192.168.") ||
			host == "127.0.0.1"
}