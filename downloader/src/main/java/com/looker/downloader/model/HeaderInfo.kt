package com.looker.downloader.model

import java.util.*

data class HeaderInfo(
	val eTag: String? = null,
	val lastModified: Date? = null
)