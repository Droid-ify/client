package com.looker.droidify.network.validation

import java.io.File

interface FileValidator {

    @Throws(ValidationException::class)
    suspend fun validate(file: File)

}
