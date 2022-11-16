package com.looker.downloader.model

data class HeaderInfo(
	val eTagChanged: Boolean,
	val eTag: String?,
	val contentLength: Long?,
	val lastModified: String?
)
