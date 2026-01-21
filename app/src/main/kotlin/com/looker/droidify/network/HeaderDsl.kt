package com.looker.droidify.network

import java.util.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import okhttp3.Request
import okhttp3.internal.http.toHttpDateString

fun Request.Builder.etag(value: String): Request.Builder =
    header("If-None-Match", value)

fun Request.Builder.ifModifiedSince(date: Date): Request.Builder =
    header("If-Modified-Since", date.toHttpDateString())

fun Request.Builder.ifModifiedSince(dateString: String): Request.Builder =
    header("If-Modified-Since", dateString)

@OptIn(ExperimentalEncodingApi::class)
fun Request.Builder.authentication(username: String, password: String): Request.Builder {
    val credentials = "$username:$password".encodeToByteArray()
    val encoded = Base64.encode(credentials)
    return header("Authorization", "Basic $encoded")
}

fun Request.Builder.authentication(base64: String): Request.Builder =
    header("Authorization", base64)

fun Request.Builder.range(start: Long): Request.Builder {
    return header("Range", "bytes=$start-")
}

fun Request.Builder.range(start: Long, end: Long? = null): Request.Builder {
    val rangeValue = if (end != null) "bytes=$start-$end" else "bytes=$start-"
    return header("Range", rangeValue)
}
