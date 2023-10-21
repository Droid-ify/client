package com.looker.droidify.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

abstract class ConnectionService<T : IBinder> : Service() {

    private val supervisorJob = SupervisorJob()
    val lifecycleScope = CoroutineScope(Dispatchers.Main + supervisorJob)

    abstract override fun onBind(intent: Intent): T

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.cancel()
    }
}
