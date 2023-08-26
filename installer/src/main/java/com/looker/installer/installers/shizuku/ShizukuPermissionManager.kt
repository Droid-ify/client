package com.looker.installer.installers.shizuku

import android.content.Context
import android.content.pm.PackageManager
import com.looker.core.common.extension.getPackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import rikka.shizuku.Shizuku

class ShizukuPermissionHandler(
	private val context: Context
) {

	fun isInstalled(): Boolean =
		context.packageManager.getPackageInfoCompat(SHIZUKU_PACKAGE_NAME) != null

	val isBinderAlive: Flow<Boolean> = callbackFlow {
		send(Shizuku.pingBinder())
		val listener = Shizuku.OnBinderReceivedListener {
			trySend(true)
		}
		Shizuku.addBinderReceivedListener(listener)
		val deadListener = Shizuku.OnBinderDeadListener {
			trySend(false)
		}
		Shizuku.addBinderDeadListener(deadListener)
		awaitClose {
			Shizuku.removeBinderReceivedListener(listener)
			Shizuku.removeBinderDeadListener(deadListener)
		}
	}.flowOn(Dispatchers.Default).conflate()

	val isGranted: Flow<Boolean> = callbackFlow {
		send(false)
		val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
			if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
				val granted = grantResult == PackageManager.PERMISSION_GRANTED
				trySend(granted)
			}
		}
		Shizuku.addRequestPermissionResultListener(listener)
		awaitClose {
			Shizuku.removeRequestPermissionResultListener(listener)
		}
	}.flowOn(Dispatchers.Default).conflate()

	fun requestPermission() {
		if (Shizuku.shouldShowRequestPermissionRationale()) {}
		Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
	}

	companion object {
		private const val SHIZUKU_PERMISSION_REQUEST_CODE = 87263
		private const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
	}
}