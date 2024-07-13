package com.looker.network.validation

class ValidationException(override val message: String) : Exception(message)

@Suppress("NOTHING_TO_INLINE")
inline fun invalid(message: Any): Nothing = throw ValidationException(message.toString())
