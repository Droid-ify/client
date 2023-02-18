package com.looker.downloader.model

import java.util.*

data class Header(
	val entityTag: String? = null,
	val lastModified: Date? = null,
	val authentication: String? = null
)
