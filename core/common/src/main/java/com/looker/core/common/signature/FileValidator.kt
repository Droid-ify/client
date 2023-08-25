package com.looker.core.common.signature

import java.io.File

interface FileValidator {

	// Throws error if not valid
	suspend fun validate(file: File)

}

class ValidationException(override val message: String) : Exception(message)