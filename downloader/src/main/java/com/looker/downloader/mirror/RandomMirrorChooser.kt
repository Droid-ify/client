package com.looker.downloader.mirror

import com.looker.downloader.model.DownloadRequest
import com.looker.downloader.model.Mirror

internal class RandomMirrorChooser : BaseMirrorChooser() {
	override fun orderedMirrors(downloadRequest: DownloadRequest): List<Mirror> =
		downloadRequest.mirrors.toMutableList().apply { shuffle() }.also { mirrors ->
			if (downloadRequest.prioritizedMirror != null) {
				mirrors.sortBy { if (it == downloadRequest.prioritizedMirror) 0 else 1 }
			}
		}
}