package com.looker.network

import java.util.Date

sealed interface NetworkResponse {

	sealed interface Error : NetworkResponse {

		data class ConnectionTimeout(val exception: Exception? = null) : Error

		data class SocketTimeout(val exception: Exception? = null) : Error

		data class IO(val exception: Exception? = null) : Error

		data class Unknown(val exception: Exception? = null) : Error

		data class Http(val statusCode: Int) : Error

	}

	data class Success(
		val statusCode: Int,
		val lastModified: Date?,
		val etag: String?
	) : NetworkResponse

}
