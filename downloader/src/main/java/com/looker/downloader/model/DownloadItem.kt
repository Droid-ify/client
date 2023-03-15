package com.looker.downloader.model

import java.io.File
import java.util.*

data class DownloadItem(
	val name: String,
	val url: String,
	val file: File,
	val id: String = UUID.randomUUID().toString(),
	val headerInfo: HeaderInfo = HeaderInfo(),
	val priority: Priority = Priority.HIGH
)
