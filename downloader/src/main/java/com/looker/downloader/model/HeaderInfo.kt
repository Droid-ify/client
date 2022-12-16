package com.looker.downloader.model

import java.util.Date

data class HeaderInfo(
	val etag: String? = null,
	val authorization: String? = null,
	val lastModified: Date? = null
)