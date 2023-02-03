package com.looker.droidify.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.looker.core.common.SdkCheck
import com.looker.droidify.database.Database
import com.looker.droidify.utility.Utils.toInstalledItem
import com.looker.droidify.utility.extension.android.Android

class InstalledAppReceiver(private val packageManager: PackageManager) : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		val packageName =
			intent.data?.let { if (it.scheme == "package") it.schemeSpecificPart else null }
		if (packageName != null) {
			when (intent.action.orEmpty()) {
				Intent.ACTION_PACKAGE_ADDED,
				Intent.ACTION_PACKAGE_REMOVED,
				-> {
					val packageInfo = try {
						if (SdkCheck.isTiramisu) {
							packageManager.getPackageInfo(
								packageName,
								PackageManager.PackageInfoFlags.of(Android.PackageManager.signaturesFlag.toLong())
							)
						} else {
							@Suppress("DEPRECATION")
							packageManager.getPackageInfo(
								packageName, Android.PackageManager.signaturesFlag
							)
						}
					} catch (e: Exception) {
						null
					}
					if (packageInfo != null) {
						Database.InstalledAdapter.put(packageInfo.toInstalledItem())
					} else {
						Database.InstalledAdapter.delete(packageName)
					}
				}
			}
		}
	}
}