package com.looker.downloader.model

import java.io.File
import java.util.*

data class DownloadItem(
	val id: String = UUID.randomUUID().toString(),
	val name: String,
	val url: String,
	val file: File,
	val authorization: String,
	val headerInfo: HeaderInfo = HeaderInfo(),
	val priority: Priority = Priority.HIGH
)

fun List<DownloadItem>.indexOfItem(id: String): Int = indexOfFirst { it.id == id }

fun Map<DownloadItem, DownloadState>.downloadState(id: String): DownloadState {
	val key = keys.first { it.id == id }
	return this[key] ?: DownloadState.Error.UnknownError
}