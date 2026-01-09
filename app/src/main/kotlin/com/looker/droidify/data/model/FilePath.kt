package com.looker.droidify.data.model

import androidx.compose.runtime.Immutable

@JvmInline
@Immutable
value class FilePath(val path: String)

fun FilePath(
    baseUrl: String,
    path: String?,
): FilePath? {
    if (path.isNullOrBlank()) return null
    val builder = cachedBaseBuilders.getOrPut(baseUrl) { StringBuilder(baseUrl) }
    val builderLength = builder.length

    if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
        builder.append("/")
    } else if (baseUrl.endsWith("/") && path.startsWith("/")) {
        builder.deleteCharAt(builder.length - 1)
    }
    builder.append(path)

    return try {
        FilePath(builder.toString())
    } finally {
        builder.setLength(builderLength)
    }
}

private val cachedBaseBuilders = mutableMapOf<String, StringBuilder>()
