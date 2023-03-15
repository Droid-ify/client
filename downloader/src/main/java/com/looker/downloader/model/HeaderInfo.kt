package com.looker.downloader.model

import java.util.Date

data class HeaderInfo(
	val etag: String? = null,
	val lastModified: Date? = null,
	val username: String? = null,
	val password: String? = null
)