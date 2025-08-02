package com.looker.droidify.data.model

import androidx.compose.runtime.Immutable

@JvmInline
@Immutable
value class FilePath(val path: String)

fun FilePath(
    baseUrl: String?,
    path: String?,
) = if (baseUrl.isNullOrBlank() || path.isNullOrBlank()) {
    null
} else {
    val builder = StringBuilder(baseUrl)
    if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
        builder.append("/")
    } else if (baseUrl.endsWith("/") && path.startsWith("/")) {
        builder.deleteCharAt(builder.length - 1)
    }
    builder.append(path)
    FilePath(builder.toString())
}
