package com.looker.network.header

import io.ktor.http.HttpHeaders
import io.ktor.util.encodeBase64
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal class KtorHeadersBuilder(
	private val builder: io.ktor.http.HeadersBuilder
) : HeadersBuilder {

	companion object {
		private val HTTP_DATE_FORMAT: SimpleDateFormat
			get() = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
				timeZone = TimeZone.getTimeZone("GMT")
			}

		private fun formatHttpDate(date: Date): String = HTTP_DATE_FORMAT.format(date)
	}

	override fun String.headsWith(value: Any?) {
		if (value == null) return
		with(builder) {
			append(this@headsWith, value.toString())
		}
	}

	override fun ifModifiedSince(date: Date) {
		HttpHeaders.IfModifiedSince headsWith formatHttpDate(date)
	}

	override fun authentication(username: String, password: String) {
		HttpHeaders.Authorization headsWith "Basic ${"$username:$password".encodeBase64()}"
	}

	override fun authentication(base64: String) {
		HttpHeaders.Authorization headsWith base64
	}

	override fun inRange(start: Number, end: Number?) {
		val valueString = if (end != null) "bytes=$start-$end" else "bytes=${start}-"
		HttpHeaders.Range headsWith valueString
	}
}