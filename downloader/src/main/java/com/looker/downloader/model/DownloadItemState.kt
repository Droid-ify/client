package com.looker.downloader.model

data class DownloaderState(
	val current: DownloadItemState,
	val queue: Set<String>
)

data class DownloadItemState(
	val item: DownloadItem,
	val state: DownloadState
) {
	companion object {
		val EMPTY = DownloadItemState(
			item = DownloadItem(url = "", fileName = ""),
			state = DownloadState.Connecting
		)
	}
}

infix fun DownloadItem.statesTo(state: DownloadState) = DownloadItemState(this, state)
