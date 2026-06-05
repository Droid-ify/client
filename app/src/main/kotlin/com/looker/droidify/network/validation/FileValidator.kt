package com.looker.droidify.network.validation

import java.io.File

interface FileValidator {

    suspend fun validate(file: File): ValidationResult

}

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: String) : ValidationResult
}
