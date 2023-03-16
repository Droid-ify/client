package com.looker.core.common.result

sealed interface Result<out T> {
	data class Success<T>(val data: T) : Result<T>

	data class Error<T>(
		val exception: Throwable? = null,
		val data: T? = null
	) : Result<T>
}