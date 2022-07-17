package com.looker.core_common.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed interface Result<out T> {
	data class Success<T>(val data: T) : Result<T>
	data class Error<T>(val exception: Throwable? = null, val data: T? = null) :
		Result<T>
	object Loading : Result<Nothing>
}

fun <T> Flow<T>.asResult(): Flow<Result<T>> {
	return this
		.map<T, Result<T>> { Result.Success(it) }
		.onStart { emit(Result.Loading) }
		.catch { emit(Result.Error(it)) }
}