package com.looker.core.data.downloader.header

import java.util.Date

interface HeadersBuilder {

	infix fun String.headsWith(value: Any?)

	fun ifModifiedSince(date: Date)

	fun authentication(username: String, password: String)

	fun authentication(base64: String)

	fun inRange(start: Number, end: Number? = null)

}