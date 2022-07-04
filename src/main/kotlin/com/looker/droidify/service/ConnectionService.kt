package com.looker.droidify.service

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import com.looker.droidify.utility.extension.android.Android

abstract class ConnectionService<T : IBinder> : LifecycleService() {
    abstract override fun onBind(intent: Intent): T

    fun startSelf() {
        val intent = Intent(this, this::class.java)
        if (Android.sdk(26)) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
