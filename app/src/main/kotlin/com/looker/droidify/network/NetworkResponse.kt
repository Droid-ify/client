package com.looker.droidify.network

import java.util.*

sealed interface NetworkResponse {

    sealed interface Error : NetworkResponse {

        data class ConnectionTimeout(val exception: Exception) : Error

        data class SocketTimeout(val exception: Exception) : Error

        data class IO(val exception: Exception) : Error

        data class Unknown(val exception: Exception) : Error

        data class Http(val statusCode: Int) : Error
    }

    data class Success(
        val statusCode: Int,
        val lastModified: Date?,
        val etag: String?,
    ) : NetworkResponse

    val isSuccess: Boolean get() = this is Success
}
