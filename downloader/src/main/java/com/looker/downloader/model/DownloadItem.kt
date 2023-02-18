package com.looker.downloader.model

import java.io.File

data class DownloadItem(
	val name: String,
	val url: String,
	val file: File,
	val header: Header
)
