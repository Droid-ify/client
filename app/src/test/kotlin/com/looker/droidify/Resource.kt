package com.looker.droidify

import java.io.File

fun assets(name: String): File? {
    val url = Thread.currentThread().contextClassLoader?.getResource(name) ?: return null
    return File(url.file)
}

