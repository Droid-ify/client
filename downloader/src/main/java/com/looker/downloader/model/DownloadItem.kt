package com.looker.downloader.model

import java.io.File

data class DownloadItem(
	val id: String,
	val name: String,
	val url: String,
	val file: File,
	val priority: Priority = Priority.HIGH
)

fun List<DownloadItem>.indexOfItem(id: String): Int = indexOfFirst { it.id == id }

fun Map<DownloadItem, DownloadState<Any>>.downloadState(id: String): DownloadState<Any> {
	val key = keys.first { it.id == id }
	return this[key] ?: DownloadState.Error.UnknownError()
}