package com.looker.network.header

import com.looker.core.common.extension.toFormattedString
import io.ktor.http.HttpHeaders
import io.ktor.util.encodeBase64
import java.util.Date

internal class KtorHeadersBuilder(
	private val builder: io.ktor.http.HeadersBuilder
) : HeadersBuilder {

	override fun String.headsWith(value: Any?) {
		if (value == null) return
		with(builder) {
			append(this@headsWith, value.toString())
		}
	}

	override fun etag(etagString: String) {
		HttpHeaders.ETag headsWith etagString
	}

	override fun ifModifiedSince(date: Date) {
		HttpHeaders.IfModifiedSince headsWith date.toFormattedString()
	}

	override fun ifModifiedSince(date: String) {
		HttpHeaders.IfModifiedSince headsWith date
	}

	override fun authentication(username: String, password: String) {
		HttpHeaders.Authorization headsWith "Basic ${"$username:$password".encodeBase64()}"
	}

	override fun authentication(base64: String) {
		HttpHeaders.Authorization headsWith base64
	}

	override fun inRange(start: Number?, end: Number?) {
		if (start == null) return
		val valueString = if (end != null) "bytes=$start-$end" else "bytes=${start}-"
		HttpHeaders.Range headsWith valueString
	}
}