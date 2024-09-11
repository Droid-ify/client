package com.looker.sync.fdroid.common

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.InputStream

fun getResource(name: String): File? {
    val url = Thread.currentThread().contextClassLoader?.getResource(name) ?: return null
    return File(url.file)
}

fun assets(name: String): InputStream {
    return InstrumentationRegistry.getInstrumentation().context.assets.open(name)
}
