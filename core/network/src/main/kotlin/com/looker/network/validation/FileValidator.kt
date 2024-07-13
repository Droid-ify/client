package com.looker.network.validation

import java.io.File

interface FileValidator {

    @Throws(ValidationException::class)
    suspend fun validate(file: File)

}
