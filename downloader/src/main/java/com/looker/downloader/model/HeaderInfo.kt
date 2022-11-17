package com.looker.downloader.model

import java.util.*

data class HeaderInfo(
	val eTag: String?,
	val contentLength: Long?,
	val lastModified: Date?
)