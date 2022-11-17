package com.looker.downloader

import com.looker.downloader.model.DownloadItem
import com.looker.downloader.model.DownloadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

sealed interface Downloader {

	suspend fun download(item: DownloadItem): Flow<DownloadState<Job>>

	suspend fun cancelDownload(item: DownloadItem): Boolean

}

data class Response(
	val lastModified: String,
	val tag: String
)