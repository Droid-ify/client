package com.looker.droidify.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.looker.core_common.sdkAbove

abstract class ConnectionService<T : IBinder> : Service() {
	abstract override fun onBind(intent: Intent): T

	fun startSelf() {
		val intent = Intent(this, this::class.java)
		sdkAbove(
			sdk = Build.VERSION_CODES.O,
			onSuccessful = { startForegroundService(intent) },
			orElse = { startService(intent) }
		)
	}
}
