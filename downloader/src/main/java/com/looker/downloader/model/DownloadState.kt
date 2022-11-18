package com.looker.downloader.model

sealed interface DownloadState {

	object Pending : DownloadState

	class Success(val headerInfo: HeaderInfo) : DownloadState

	data class Progress(val total: Long, val current: Long, val percent: Int) : DownloadState

	sealed interface Error : DownloadState {
		object UnknownError : Error
		data class HttpError(val code: Int, val exception: Exception) : Error
		data class IOError(val exception: Exception) : Error
	}
}