package com.looker.droidify.network.header

import io.ktor.http.HttpHeaders
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.encoding.Base64

internal class KtorHeadersBuilder(
    private val builder: io.ktor.http.HeadersBuilder,
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
        HttpHeaders.Authorization headsWith "Basic ${Base64.encode("$username:$password".encodeToByteArray())}"
    }

    override fun authentication(base64: String) {
        HttpHeaders.Authorization headsWith base64
    }

    override fun inRange(start: Long, end: Long?) {
        val valueString = if (end != null) "bytes=$start-$end" else "bytes=$start-"
        HttpHeaders.Range headsWith valueString
    }

    private companion object {
        val HTTP_DATE_FORMAT: SimpleDateFormat
            get() = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
            }

        fun Date.toFormattedString(): String = HTTP_DATE_FORMAT.format(this)
    }
}
