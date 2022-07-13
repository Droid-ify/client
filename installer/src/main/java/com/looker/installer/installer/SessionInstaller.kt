package com.looker.installer.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import androidx.core.net.toFile
import com.looker.installer.utils.BaseInstaller
import com.looker.installer.InstallerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class SessionInstaller(context: Context) : BaseInstaller(context) {

	private val sessionInstaller = context.packageManager.packageInstaller
	private val intent = Intent(context, InstallerService::class.java)

	companion object {
		val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
	}

	override suspend fun install(packageName: String, uri: Uri, file: File) {
		mDefaultInstaller(file)
	}

	override suspend fun uninstall(packageName: String) = mDefaultUninstaller(packageName)

	private fun mDefaultInstaller(cacheFile: File) {
		val sessionParams =
			PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			sessionParams.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
		}
		val id = sessionInstaller.createSession(sessionParams)

		val session = sessionInstaller.openSession(id)

		session.use { activeSession ->
			activeSession.openWrite("package", 0, cacheFile.length()).use { packageStream ->
				cacheFile.inputStream().use { fileStream ->
					fileStream.copyTo(packageStream)
				}
			}

			val pendingIntent = PendingIntent.getService(context, id, intent, flags)

			session.commit(pendingIntent.intentSender)
		}
	}

	private suspend fun mDefaultUninstaller(packageName: String) {
		intent.putExtra(InstallerService.KEY_ACTION, InstallerService.ACTION_UNINSTALL)

		val pendingIntent = PendingIntent.getService(context, -1, intent, flags)

		withContext(Dispatchers.IO) {
			sessionInstaller.uninstall(packageName, pendingIntent.intentSender)
		}
	}
}