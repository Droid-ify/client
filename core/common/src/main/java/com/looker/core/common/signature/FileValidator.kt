package com.looker.core.common.signature

import java.io.File

interface FileValidator {

    // Throws error if not valid
    @Throws(ValidationException::class)
    suspend fun validate(file: File)
}

class ValidationException(override val message: String) : Exception(message)

@Suppress("NOTHING_TO_INLINE")
inline fun invalid(message: Any): Nothing = throw ValidationException(message.toString())
