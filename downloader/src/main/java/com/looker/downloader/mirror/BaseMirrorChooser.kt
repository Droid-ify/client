package com.looker.downloader.mirror

import android.util.Log
import com.looker.downloader.model.DownloadRequest
import com.looker.downloader.model.Mirror
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.Url
import io.ktor.utils.io.errors.IOException

abstract class BaseMirrorChooser : MirrorChooser {

	override suspend fun <T> mirrorRequest(
		downloadRequest: DownloadRequest,
		request: suspend (mirror: Mirror, url: Url) -> T
	): T {
		val mirrors = if (downloadRequest.proxy == null) {
			val orderedMirrors = orderedMirrors(downloadRequest).filter { !it.isOnion() }
			orderedMirrors.ifEmpty { downloadRequest.mirrors }
		} else {
			orderedMirrors(downloadRequest)
		}
		mirrors.forEachIndexed { index, mirror ->
			val url = mirror.getUrl(downloadRequest.path)
			try {
				return request(mirror, url)
			} catch (e: ResponseException) {
				if (downloadRequest.hasCredential && e.response.status == Forbidden) throw e
				if (downloadRequest.prioritizedMirror != null && e.response.status == NotFound) throw e
				// also throw if this is the last mirror to try, otherwise try next
				throwOnLastMirror(e, index == downloadRequest.mirrors.size - 1)
			} catch (e: IOException) {
				throwOnLastMirror(e, index == downloadRequest.mirrors.size - 1)
			}
		}
		error("Unreachable Code Reached in BaseMirrorChooser")
	}

	private fun throwOnLastMirror(e: Exception, wasLastMirror: Boolean) {
		Log.w(
			"BaseMirrorChooser", if (wasLastMirror) "Last mirror, rethrowing..."
			else "Trying other mirror now..."
		)
		if (wasLastMirror) throw e
	}

}