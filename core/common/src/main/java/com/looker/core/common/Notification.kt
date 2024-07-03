package com.looker.core.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.looker.core.common.extension.notificationManager

fun Context.createNotificationChannel(
    id: String,
    name: String,
    description: String? = null,
    showBadge: Boolean = false,
) {
    sdkAbove(Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            id,
            name,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setDescription(description)
            setShowBadge(showBadge)
            setSound(null, null)
        }
        notificationManager?.createNotificationChannel(channel)
    }
}
