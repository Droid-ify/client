package com.looker.network.header

import java.util.Date

interface HeadersBuilder {

    infix fun String.headsWith(value: Any?)

    fun etag(etagString: String)

    fun ifModifiedSince(date: Date)

    fun ifModifiedSince(date: String)

    fun authentication(username: String, password: String)

    fun authentication(base64: String)

    fun inRange(start: Number?, end: Number? = null)
}
