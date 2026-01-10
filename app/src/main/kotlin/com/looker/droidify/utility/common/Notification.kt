package com.looker.droidify.utility.common

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ForegroundInfo
import com.looker.droidify.utility.common.extension.notificationManager

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

fun Notification.toForegroundInfo(
    id: Int,
    type: Int = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    ForegroundInfo(id, this, type)
} else {
    ForegroundInfo(id, this)
}

