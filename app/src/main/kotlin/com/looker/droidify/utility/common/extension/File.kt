package com.looker.droidify.utility.common.extension

import android.content.Context
import com.looker.droidify.utility.common.cache.Cache
import java.io.File

val File.size: Long?
    get() = if (exists()) length().takeIf { it > 0L } else null

inline fun Context.tempFile(block: (file: File) -> Unit) {
    Cache.getTemporaryFile(this).also { file ->
        block(file)
        file.delete()
    }
}

