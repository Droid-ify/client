package com.looker.network

sealed interface NetworkResponse {
	data class Error(
		val statusCode: Int,
		val exception: Exception? = null
	) : NetworkResponse

	object Success : NetworkResponse
}
