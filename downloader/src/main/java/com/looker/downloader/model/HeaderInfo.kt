package com.looker.downloader.model

import java.util.*

data class HeaderInfo(
	val etag: String? = null,
	val lastModified: Date? = null,
	val authentication: String? = null
)