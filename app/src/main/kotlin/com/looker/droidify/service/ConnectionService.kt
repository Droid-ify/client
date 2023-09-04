package com.looker.droidify.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.*

abstract class ConnectionService<T : IBinder> : Service() {

	private val supervisorJob = SupervisorJob()
	val lifecycleScope = CoroutineScope(Dispatchers.Main + supervisorJob)

	abstract override fun onBind(intent: Intent): T

	override fun onDestroy() {
		super.onDestroy()
		lifecycleScope.cancel()
	}
}
