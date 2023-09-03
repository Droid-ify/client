package com.looker.droidify.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

abstract class ConnectionService<T : IBinder> : Service() {
	abstract override fun onBind(intent: Intent): T
}
