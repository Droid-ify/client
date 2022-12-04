package com.looker.downloader.model

data class HeaderInfo(
	val etag: String? = null,
	val authorization: String? = null,
	val lastModified: String? = null
)