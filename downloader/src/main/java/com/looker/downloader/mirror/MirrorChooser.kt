package com.looker.downloader.mirror

import com.looker.downloader.model.DownloadRequest
import com.looker.downloader.model.Mirror
import io.ktor.http.*

interface MirrorChooser {

	fun orderedMirrors(downloadRequest: DownloadRequest): List<Mirror>

	suspend fun <T> mirrorRequest(
		downloadRequest: DownloadRequest,
		request: suspend (mirror: Mirror, url: Url) -> T
	): T

}