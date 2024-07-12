package com.looker.core.data.fdroid.sync.workers

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.looker.core.common.createNotificationChannel
import com.looker.core.common.R as CommonR

private const val SyncNotificationID = 12
private const val SyncNotificationChannelID = "SyncNotificationChannelID"

fun Context.syncForegroundInfo() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    ForegroundInfo(
        SyncNotificationID,
        syncWorkNotification(),
        FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )
} else {
    ForegroundInfo(
        SyncNotificationID,
        syncWorkNotification(),
    )
}

private fun Context.syncWorkNotification(): Notification {
    createNotificationChannel(
        id = SyncNotificationChannelID,
        name = getString(CommonR.string.sync_repositories),
        description = getString(CommonR.string.sync_repositories),
    )
    return NotificationCompat.Builder(
        this,
        SyncNotificationChannelID
    )
        .setSmallIcon(CommonR.drawable.ic_sync)
        .setContentTitle(getString(CommonR.string.syncing))
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
}
