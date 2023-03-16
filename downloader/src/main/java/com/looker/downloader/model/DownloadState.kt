package com.looker.downloader.model

import com.looker.core.common.extension.percentBy
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import java.io.IOException

sealed interface DownloadState {

	object Connecting : DownloadState

	class Success(val headerInfo: HeaderInfo) : DownloadState

	data class Progress(
		val current: Long,
		val total: Long,
		val percent: Int = current percentBy total
	) : DownloadState

	sealed interface Error : DownloadState {
		object UnknownError : Error

		@JvmInline
		value class RedirectError(val exception: RedirectResponseException) : Error

		@JvmInline
		value class ClientError(val exception: ClientRequestException) : Error

		@JvmInline
		value class ServerError(val exception: ServerResponseException) : Error

		@JvmInline
		value class IOError(val exception: IOException) : Error
	}
}