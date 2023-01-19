package com.looker.droidify.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.looker.core.common.SdkCheck

abstract class ConnectionService<T : IBinder> : Service() {
	abstract override fun onBind(intent: Intent): T

	fun startSelf() {
		val intent = Intent(this, this::class.java)
		if (SdkCheck.isOreo) startForegroundService(intent)
		else startService(intent)
	}
}
