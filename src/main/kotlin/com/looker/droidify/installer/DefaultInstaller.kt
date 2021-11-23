package com.looker.droidify.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller.SessionParams
import com.looker.droidify.content.Cache
import com.looker.droidify.utility.extension.android.Android
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DefaultInstaller(context: Context) : BaseInstaller(context) {
    override suspend fun install(cacheFileName: String) {
        val cacheFile = Cache.getReleaseFile(context, cacheFileName)
        mDefaultInstaller(cacheFile)
    }

    override suspend fun install(packageName: String, cacheFileName: String) {
        val cacheFile = Cache.getReleaseFile(context, cacheFileName)
        mDefaultInstaller(cacheFile)
    }

    override suspend fun install(packageName: String, cacheFile: File) =
        mDefaultInstaller(cacheFile)

    override suspend fun uninstall(packageName: String) = mDefaultUninstaller(packageName)

    private fun mDefaultInstaller(cacheFile: File) {
        val sessionInstaller = context.packageManager.packageInstaller
        val sessionParams =
            SessionParams(SessionParams.MODE_FULL_INSTALL)

        if (Android.sdk(31)) {
            sessionParams.setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
        }

        val id = sessionInstaller.createSession(sessionParams)

        val session = sessionInstaller.openSession(id)

        session.use { activeSession ->
            activeSession.openWrite("package", 0, cacheFile.length()).use { packageStream ->
                cacheFile.inputStream().use { fileStream ->
                    fileStream.copyTo(packageStream)
                }
            }

            val intent = Intent(context, InstallerService::class.java)

            val flags = if (Android.sdk(31)) PendingIntent.FLAG_MUTABLE else 0

            val pendingIntent = PendingIntent.getService(context, id, intent, flags)

            session.commit(pendingIntent.intentSender)
        }
    }

    private suspend fun mDefaultUninstaller(packageName: String) {
        val sessionInstaller = context.packageManager.packageInstaller

        val intent = Intent(context, InstallerService::class.java)
        intent.putExtra(InstallerService.KEY_ACTION, InstallerService.ACTION_UNINSTALL)

        val flags = if (Android.sdk(31)) PendingIntent.FLAG_MUTABLE else 0

        val pendingIntent = PendingIntent.getService(context, -1, intent, flags)

        withContext(Dispatchers.IO) {
            sessionInstaller.uninstall(packageName, pendingIntent.intentSender)
        }
    }
}