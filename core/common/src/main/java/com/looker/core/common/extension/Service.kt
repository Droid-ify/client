package com.looker.core.common.extension

import android.app.Service
import android.content.Intent
import com.looker.core.common.SdkCheck

fun Service.startSelf() {
    val intent = Intent(this, this::class.java)
    if (SdkCheck.isOreo) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}

fun Service.stopForegroundCompat(removeNotification: Boolean = true) {
    @Suppress("DEPRECATION")
    if (SdkCheck.isNougat) {
        stopForeground(
            if (removeNotification) Service.STOP_FOREGROUND_REMOVE else Service.STOP_FOREGROUND_DETACH
        )
    } else {
        stopForeground(removeNotification)
    }
    stopSelf()
}
