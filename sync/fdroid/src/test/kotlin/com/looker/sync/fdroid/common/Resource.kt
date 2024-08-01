package com.looker.sync.fdroid.common

import java.io.File

fun getResource(name: String): File? {
    val url = Thread.currentThread().contextClassLoader.getResource(name) ?: return null
    return File(url.file)
}
