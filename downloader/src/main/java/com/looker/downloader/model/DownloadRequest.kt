package com.looker.downloader.model

import io.ktor.client.engine.*

data class DownloadRequest(
	val path: String,
	val mirrors: List<Mirror>,
	val proxy: ProxyConfig? = null,
	val userName: String? = null,
	val password: String? = null,
	val prioritizedMirror: Mirror? = null
) {
	val hasCredential: Boolean = userName != null && password != null
}