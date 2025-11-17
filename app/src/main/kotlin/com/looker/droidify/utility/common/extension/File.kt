package com.looker.droidify.utility.common.extension

import android.content.Context
import com.looker.droidify.utility.common.cache.Cache
import java.io.File

val File.size: Long?
    get() = if (exists()) length().takeIf { it > 0L } else null

inline fun <T> Context.tempFile(block: (file: File) -> T): T {
    val file = Cache.getTemporaryFile(this)
    return try {
        block(file)
    } finally {
        file.delete()
    }
}

