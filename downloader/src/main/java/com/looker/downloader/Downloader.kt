package com.looker.downloader

import com.looker.downloader.model.DownloadItem
import com.looker.downloader.model.DownloadState
import kotlinx.coroutines.flow.Flow

interface Downloader {

	suspend fun download(item: DownloadItem): Flow<DownloadState>

}