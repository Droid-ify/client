package com.looker.downloader.model

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import java.io.IOException

sealed interface DownloadState {

	object Pending : DownloadState

	object Success : DownloadState

	data class Progress(val total: Long, val current: Long, val percent: Int) : DownloadState

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