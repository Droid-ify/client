package com.looker.droidify.network

sealed interface DownloadState {

	object Pending : DownloadState

	data class Progress(val read: Long, val total: Long) : DownloadState

	data class Success(val lastModified: String, val etag: String) : DownloadState

	object Error : DownloadState

}