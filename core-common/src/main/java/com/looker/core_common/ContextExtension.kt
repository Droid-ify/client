package com.looker.core_common

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.NotificationCompat

val Context.notificationManager: NotificationManager
	get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

/**
 * Helper method to create a notification builder.
 *
 * @param id the channel id.
 * @param block the function that will execute inside the builder.
 * @return a notification to be displayed or updated.
 */
fun Context.notificationBuilder(
	channelId: String,
	block: (NotificationCompat.Builder.() -> Unit)? = null
): NotificationCompat.Builder {
	val builder = NotificationCompat.Builder(this, channelId)
	if (block != null) builder.block()
	return builder
}

fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
	val className = serviceClass.name
	val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
	@Suppress("DEPRECATION")
	return manager.getRunningServices(Integer.MAX_VALUE)
		.any { className == it.service.className }
}

fun Context.isPackageInstalled(packageName: String): Boolean {
	return try {
		packageManager.getApplicationInfo(packageName, 0)
		true
	} catch (e: PackageManager.NameNotFoundException) {
		false
	}
}

fun Context.getInstallerPackageName(): String? {
	return try {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			packageManager.getInstallSourceInfo(packageName).installingPackageName
		} else {
			@Suppress("DEPRECATION")
			packageManager.getInstallerPackageName(packageName)
		}
	} catch (e: Exception) {
		null
	}
}

fun Context.getApplicationIcon(pkgName: String): Drawable? {
	return try {
		packageManager.getApplicationIcon(pkgName)
	} catch (e: PackageManager.NameNotFoundException) {
		null
	}
}