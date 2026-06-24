package com.looker.droidify.utility.common.extension

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.util.Log
import com.looker.droidify.utility.common.SdkCheck

fun Service.startServiceCompat() {
    val intent = Intent(this, this::class.java)
    if (SdkCheck.isOreo) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}

fun Service.stopForegroundCompat(removeNotification: Boolean = true) {
    if (SdkCheck.isNougat) {
        stopForeground(
            if (removeNotification) {
                Service.STOP_FOREGROUND_REMOVE
            } else {
                Service.STOP_FOREGROUND_DETACH
            },
        )
    } else {
        @Suppress("DEPRECATION")
        stopForeground(removeNotification)
    }
    stopSelf()
}

// `ForegroundServiceStartNotAllowedException` (API 31+) and
// `BackgroundServiceStartNotAllowedException` both extend `IllegalStateException`. The Android 15
// `dataSync` daily quota raises the former from inside `startForeground`, which would otherwise
// kill the process. Returns `true` on success, `false` if the call was denied — callers handle
// the false case (drop tasks, stop the service, etc.).
fun Service.startForegroundSafe(id: Int, notification: Notification): Boolean = try {
    startForeground(id, notification)
    true
} catch (e: IllegalStateException) {
    Log.w("Service", "startForeground denied: ${e::class.java.simpleName}: ${e.message}")
    false
}
