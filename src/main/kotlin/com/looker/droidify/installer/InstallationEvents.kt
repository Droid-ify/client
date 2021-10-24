package com.looker.droidify.installer

import java.io.File

interface InstallationEvents {
    fun install(packageName: String, cacheFileName: String)

    fun install(packageName: String, cacheFile: File)

    fun uninstall(packageName: String)
}