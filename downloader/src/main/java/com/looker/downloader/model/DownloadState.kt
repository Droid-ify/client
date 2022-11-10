package com.looker.downloader.model

sealed interface DownloadState<out T> {

	class Pending<T> : DownloadState<T>

	class Success<T>(val data: T) : DownloadState<T>

	data class Progress<T>(val total: Long, val percent: Int) : DownloadState<T>

	sealed interface Error<T> : DownloadState<T> {
		class UnknownError<T> : Error<T>
		data class HttpError<T>(val code: Int, val exception: Exception) : Error<T>
		data class IOError<T>(val exception: Exception) : Error<T>
	}
}