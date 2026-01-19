package com.looker.droidify.ui

import com.looker.droidify.network.DataSize

/**
 * Represents the current download/install status of an app.
 *
 * This is a unified status model used by both [AppDetailAdapter] and [AppListAdapter]
 * to display progress indicators. The status flows through these states:
 *
 * ```
 * Idle -> Pending -> Connecting -> Downloading -> PendingInstall -> Installing -> Idle
 * ```
 *
 * Note: [Pending] is for download queue, [PendingInstall] is for install queue.
 * These are distinct states with different visual indicators.
 */
sealed interface DownloadStatus {
    /** No active download or installation */
    data object Idle : DownloadStatus

    /** Waiting in download queue (download not yet started) */
    data object Pending : DownloadStatus

    /** Establishing connection to download server */
    data object Connecting : DownloadStatus

    /** Actively downloading with progress information */
    data class Downloading(val read: DataSize, val total: DataSize?) : DownloadStatus

    /** Download complete, waiting in install queue */
    data object PendingInstall : DownloadStatus

    /** Package installer is running */
    data object Installing : DownloadStatus
}
