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
import kotlinx.coroutines.flow.MutableStateFlow

private const val APK_MIME = "application/vnd.android.package-archive"

internal suspend fun Context.installItemLegacy(
	installItem: InstallItem,
	state: MutableStateFlow<InstallItemState>
): Boolean {
	val (uri, flags) = if (SdkCheck.isNougat) {
		Cache.getReleaseUri(
			this,
			installItem.installFileName
		) to Intent.FLAG_GRANT_READ_URI_PERMISSION
	} else {
		val file = Cache.getReleaseFile(this, installItem.installFileName)
		file.toUri() to 0
	}
	return try {
		state.emit(InstallItemState(installItem, InstallState.Installing))
		startActivity(
			Intent(Intent.ACTION_INSTALL_PACKAGE)
				.setDataAndType(uri, APK_MIME)
				.setFlags(flags)
		)
		state.emit(InstallItemState(installItem, InstallState.Installed))
		true
	} catch (e: AndroidRuntimeException) {
		startActivity(
			Intent(Intent.ACTION_INSTALL_PACKAGE)
				.setDataAndType(uri, APK_MIME)
				.setFlags(flags or Intent.FLAG_ACTIVITY_NEW_TASK)
		)
		state.emit(InstallItemState(installItem, InstallState.Installed))
		true
	} catch (e: Exception) {
		state.emit(InstallItemState(installItem, InstallState.Failed))
		false
	}

}

internal fun Context.uninstallItemLegacy(packageName: PackageName) {
	startActivity(
		Intent(Intent.ACTION_UNINSTALL_PACKAGE, "package:${packageName.name}".toUri())
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
	)
}