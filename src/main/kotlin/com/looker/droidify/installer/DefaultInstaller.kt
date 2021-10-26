package com.looker.droidify.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.looker.droidify.content.Cache
import com.looker.droidify.utility.extension.android.Android
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DefaultInstaller(context: Context) : BaseInstaller(context) {

    override fun install(packageName: String, cacheFileName: String) {
        val cacheFile = Cache.getReleaseFile(context, cacheFileName)
        scope.launch { mDefaultInstaller(cacheFile) }
    }

    override fun install(packageName: String, cacheFile: File) {
        scope.launch { mDefaultInstaller(cacheFile) }
    }

    override fun uninstall(packageName: String) {
        scope.launch { mDefaultUninstaller(packageName) }
    }

    private suspend fun mDefaultInstaller(cacheFile: File) {
        val (uri, flags) = if (Android.sdk(24)) {
            Pair(
                Cache.getReleaseUri(context, cacheFile.name),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } else {
            Pair(Uri.fromFile(cacheFile), 0)
        }
        // TODO Handle deprecation
        @Suppress("DEPRECATION")
        withContext(Dispatchers.IO) {
            context.startActivity(
                Intent(Intent.ACTION_INSTALL_PACKAGE)
                    .setDataAndType(uri, "application/vnd.android.package-archive").setFlags(flags)
            )
        }
    }

    private fun mDefaultUninstaller(packageName: String) {
        val uri = Uri.fromParts("package", packageName, null)
        val intent = Intent()
        intent.data = uri
        if (Android.sdk(28)) {
            intent.action = Intent.ACTION_DELETE
        } else {
            intent.action = Intent.ACTION_UNINSTALL_PACKAGE
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}