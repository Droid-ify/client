package com.looker.network

import java.util.Date

sealed interface NetworkResponse {
	data class Error(
		val statusCode: Int,
		val exception: Exception? = null
	) : NetworkResponse

	data class Success(
		val statusCode: Int,
		val lastModified: Date?,
		val etag: String?
	) : NetworkResponse
}
