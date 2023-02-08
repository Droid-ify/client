package com.looker.installer.installers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.looker.core.common.SdkCheck
import com.looker.core.common.cache.Cache
import com.looker.core.common.sdkAbove
import com.looker.core.model.newer.PackageName
import com.looker.installer.model.InstallItem
import com.looker.installer.model.InstallItemState
import com.looker.installer.model.InstallState
import com.looker.installer.model.statesTo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

internal class SessionInstaller(private val context: Context) : BaseInstaller {

	private val sessionInstaller = context.packageManager.packageInstaller
	private val intent = Intent(context, SessionInstallerService::class.java)

	companion object {
		private val flags = if (SdkCheck.isSnowCake) PendingIntent.FLAG_MUTABLE else 0
	}

	override suspend fun performInstall(
		installItem: InstallItem,
		state: MutableStateFlow<InstallItemState>
	) {
		state.emit(installItem statesTo InstallState.Installing)
		val cacheFile = Cache.getReleaseFile(context, installItem.installFileName)
		val sessionParams =
			PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
		sdkAbove(sdk = Build.VERSION_CODES.S) {
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
		state.emit(installItem statesTo InstallState.Installed)
	}

	@SuppressLint("MissingPermission")
	override suspend fun performUninstall(packageName: PackageName) =
		suspendCancellableCoroutine<Unit> {
			intent.putExtra(
				SessionInstallerService.KEY_ACTION,
				SessionInstallerService.ACTION_UNINSTALL
			)
			val pendingIntent = PendingIntent.getService(context, -1, intent, flags)

			sessionInstaller.uninstall(packageName.name, pendingIntent.intentSender)
		}
}