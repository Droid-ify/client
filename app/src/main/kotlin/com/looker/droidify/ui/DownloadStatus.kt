package com.looker.droidify.ui

import com.looker.droidify.network.DataSize

sealed interface DownloadStatus {
    data object Idle : DownloadStatus
    data object Pending : DownloadStatus
    data object Connecting : DownloadStatus
    data class Downloading(val read: DataSize, val total: DataSize?) : DownloadStatus
    data object PendingInstall : DownloadStatus
    data object Installing : DownloadStatus
}
