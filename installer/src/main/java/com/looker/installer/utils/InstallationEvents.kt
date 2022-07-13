package com.looker.installer.utils

import android.net.Uri
import java.io.File

internal interface InstallationEvents {

	suspend fun install(packageName: String, uri: Uri, file: File)

	suspend fun uninstall(packageName: String)

}