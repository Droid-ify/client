package com.looker.core_common.result

sealed interface DownloadResult<out T> {
	data class Success<T>(val data: T) : DownloadResult<T>
	data class Error(val exception: Throwable? = null) : DownloadResult<Nothing>
	data class Downloading<T>(val progress: Long, val total: Long) : DownloadResult<T>
	object Loading : DownloadResult<Nothing>
}
