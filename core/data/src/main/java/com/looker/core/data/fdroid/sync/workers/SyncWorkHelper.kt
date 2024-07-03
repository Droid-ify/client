package com.looker.core.data.fdroid.sync.workers

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.looker.core.common.createNotificationChannel
import com.looker.core.common.R as CommonR

private const val SyncNotificationID = 12
private const val SyncNotificationChannelID = "SyncNotificationChannelID"

fun Context.syncForegroundInfo() = ForegroundInfo(
    SyncNotificationID,
    syncWorkNotification()
)

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
