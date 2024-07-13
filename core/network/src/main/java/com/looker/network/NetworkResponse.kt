package com.looker.network

import com.looker.network.validation.ValidationException
import java.util.Date

sealed interface NetworkResponse {

    sealed interface Error : NetworkResponse {

        data class ConnectionTimeout(val exception: Exception) : Error

        data class SocketTimeout(val exception: Exception) : Error

        data class IO(val exception: Exception) : Error

        data class Validation(val exception: ValidationException) : Error

        data class Unknown(val exception: Exception) : Error

        data class Http(val statusCode: Int) : Error
    }

    data class Success(
        val statusCode: Int,
        val lastModified: Date?,
        val etag: String?
    ) : NetworkResponse
}
