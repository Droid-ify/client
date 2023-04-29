package com.looker.core.data.downloader

sealed interface NetworkResponse {
	data class Error(
		val statusCode: Int,
		val exception: Exception? = null
	) : NetworkResponse

	object Success : NetworkResponse
}
