package com.looker.installer.installers

import android.content.Context
import android.content.Intent
import android.util.AndroidRuntimeException
import androidx.core.net.toUri
import com.looker.core.common.SdkCheck
import com.looker.core.common.cache.Cache
import com.looker.core.model.newer.PackageName
import com.looker.installer.model.InstallItem
import com.looker.installer.model.InstallItemState
import com.looker.installer.model.InstallState
import com.looker.installer.model.statesTo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Suppress("DEPRECATION")
internal class LegacyInstaller(private val context: Context) : BaseInstaller {

	companion object {
		private const val APK_MIME = "application/vnd.android.package-archive"
	}

	override suspend fun performInstall(
		installItem: InstallItem,
		state: MutableStateFlow<InstallItemState>
	) {
		val (uri, flags) = if (SdkCheck.isNougat) {
			Cache.getReleaseUri(
				context,
				installItem.installFileName
			) to Intent.FLAG_GRANT_READ_URI_PERMISSION
		} else {
			val file = Cache.getReleaseFile(context, installItem.installFileName)
			file.toUri() to 0
		}
		try {
			context.startActivity(
				Intent(Intent.ACTION_INSTALL_PACKAGE)
					.setDataAndType(uri, APK_MIME)
					.setFlags(flags)
			)
			state.emit(installItem statesTo InstallState.Installed)
		} catch (e: AndroidRuntimeException) {
			context.startActivity(
				Intent(Intent.ACTION_INSTALL_PACKAGE)
					.setDataAndType(uri, APK_MIME)
					.setFlags(flags or Intent.FLAG_ACTIVITY_NEW_TASK)
			)
			state.emit(installItem statesTo InstallState.Installed)
		} catch (e: Exception) {
			state.emit(installItem statesTo InstallState.Failed)
		}
	}

	override suspend fun performUninstall(packageName: PackageName) =
		context.uninstallPackage(packageName)

	override fun cleanup() {}
}

internal suspend fun Context.uninstallPackage(packageName: PackageName) =
	suspendCancellableCoroutine {
		startActivity(
			Intent(Intent.ACTION_UNINSTALL_PACKAGE, "package:${packageName.name}".toUri())
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		)
		it.resume(Unit)
	}