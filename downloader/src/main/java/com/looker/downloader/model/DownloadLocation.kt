package com.looker.downloader.model

import java.io.File

data class DownloadLocation(
	val url: String,
	val file: File,
	val headerInfo: HeaderInfo = HeaderInfo()
)

fun DownloadLocation.toItem() = DownloadItem(
	url = url,
	fileName = file.name,
	headerInfo = headerInfo
)