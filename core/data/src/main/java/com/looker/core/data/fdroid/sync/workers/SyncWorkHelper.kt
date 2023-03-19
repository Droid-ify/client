package com.looker.core.data.fdroid.sync.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.looker.core.common.SdkCheck
import com.looker.core.common.R as CommonR

private const val SyncNotificationID = 12
private const val SyncNotificationChannelID = "SyncNotificationChannelID"

fun Context.syncForegroundInfo() = ForegroundInfo(
	SyncNotificationID,
	syncWorkNotification()
)

private fun Context.syncWorkNotification(): Notification {
	if (SdkCheck.isOreo) {
		val channel = NotificationChannel(
			SyncNotificationChannelID,
			getString(CommonR.string.sync_repositories),
			NotificationManager.IMPORTANCE_LOW,
		).apply {
			description = getString(CommonR.string.sync_repositories)
		}
		// Register the channel with the system
		val notificationManager: NotificationManager? =
			getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

		notificationManager?.createNotificationChannel(channel)
	}

	return NotificationCompat.Builder(
		this,
		SyncNotificationChannelID,
	)
		.setSmallIcon(CommonR.drawable.ic_sync)
		.setContentTitle(getString(CommonR.string.syncing))
		.setPriority(NotificationCompat.PRIORITY_LOW)
		.build()
}