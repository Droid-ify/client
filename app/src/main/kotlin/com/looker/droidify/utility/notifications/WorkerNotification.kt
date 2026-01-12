package com.looker.droidify.utility.notifications

import android.app.Notification
import android.content.Context
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.looker.droidify.R
import com.looker.droidify.utility.common.Constants.NOTIFICATION_CHANNEL_SYNCING

fun Context.createDownloadStatsNotification(): Notification {
    return NotificationCompat
        .Builder(this, NOTIFICATION_CHANNEL_SYNCING)
        .apply {
            setOngoing(false)
            setOnlyAlertOnce(true)
            setColor(Color.GREEN)
            setSmallIcon(android.R.drawable.stat_sys_download)
            setContentTitle(getString(R.string.downloading))
            setContentText(getString(R.string.downloading_download_stats))
            setProgress(-1, -1, true)
        }
        .build()
}

fun Context.createRbNotification(): Notification {
    return NotificationCompat
        .Builder(this, NOTIFICATION_CHANNEL_SYNCING)
        .apply {
            setOngoing(false)
            setOnlyAlertOnce(true)
            setColor(Color.GREEN)
            setSmallIcon(android.R.drawable.stat_sys_download)
            setContentTitle(getString(R.string.downloading))
            setContentText(getString(R.string.downloading_rb_logs))
            setProgress(-1, -1, true)
        }
        .build()
}
