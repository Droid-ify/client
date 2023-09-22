package com.looker.installer.installers.shizuku

import android.content.Context
import android.content.pm.PackageManager
import com.looker.core.common.extension.getPackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

class ShizukuPermissionHandler(
	private val context: Context
) {

	fun isInstalled(): Boolean =
		context.packageManager.getPackageInfoCompat(ShizukuProvider.MANAGER_APPLICATION_ID) != null

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
	}.flowOn(Dispatchers.Default).distinctUntilChanged().conflate()

	val isGranted: Flow<Boolean> = callbackFlow {
		if (Shizuku.pingBinder()) {
			send(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
		} else {
			send(false)
		}
		val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
			if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
				trySend(grantResult == PackageManager.PERMISSION_GRANTED)
			}
		}
		Shizuku.addRequestPermissionResultListener(listener)
		awaitClose {
			Shizuku.removeRequestPermissionResultListener(listener)
		}
	}.flowOn(Dispatchers.Default).distinctUntilChanged().conflate()

	fun requestPermission() {
		if (Shizuku.shouldShowRequestPermissionRationale()) {
		}
		Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
	}

	val state: Flow<State> = combine(
		flowOf(isInstalled()),
		isBinderAlive,
		isGranted
	) { isInstalled, isAlive, isGranted ->
		State(isGranted, isAlive, isInstalled)
	}.distinctUntilChanged()

	companion object {
		private const val SHIZUKU_PERMISSION_REQUEST_CODE = 87263
	}

	data class State(
		val isPermissionGranted: Boolean,
		val isAlive: Boolean,
		val isInstalled: Boolean
	)
}