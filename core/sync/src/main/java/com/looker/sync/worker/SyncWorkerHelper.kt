package com.looker.sync.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import com.looker.core.datastore.UserPreferences
import com.looker.core.datastore.model.ProxyType
import com.looker.core.common.R as CommonR

private const val SyncNotificationId = 0
private const val SyncNotificationChannelID = "SyncNotificationChannel"

// All sync work needs an internet connectionS
val SyncConstraints
	get() = Constraints.Builder()
		.setRequiredNetworkType(NetworkType.CONNECTED)
		.build()

/**
 * Foreground information for sync on lower API levels when sync workers are being
 * run with a foreground service
 */
fun Context.syncForegroundInfo() = ForegroundInfo(
	SyncNotificationId,
	syncWorkNotification(),
)

/**
 * Notification displayed on lower API levels when sync workers are being
 * run with a foreground service
 */
private fun Context.syncWorkNotification(): Notification {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		val channel = NotificationChannel(
			SyncNotificationChannelID,
			getString(CommonR.string.sync_repositories),
			NotificationManager.IMPORTANCE_DEFAULT,
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
		.setSmallIcon(CommonR.drawable.ic_sync,)
		.setContentTitle(getString(CommonR.string.syncing))
		.setPriority(NotificationCompat.PRIORITY_DEFAULT)
		.build()
}