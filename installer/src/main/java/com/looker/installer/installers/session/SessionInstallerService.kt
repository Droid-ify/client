package com.looker.installer.installers.session

import android.app.Service
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.IBinder
import android.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import com.looker.core.common.Constants.NOTIFICATION_CHANNEL_DOWNLOADING
import com.looker.core.common.Constants.NOTIFICATION_ID_DOWNLOADING
import com.looker.core.common.R as CommonR
import com.looker.core.common.extension.notificationManager

class SessionInstallerService : Service() {
    companion object {
        const val ACTION_UNINSTALL = "action_uninstall"
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            // prompts user to enable unknown source
            val promptIntent: Intent? = intent.getParcelableExtra(Intent.EXTRA_INTENT)

            promptIntent?.let {
                it.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                it.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                startActivity(it)
            }
        } else {
            notifyStatus(intent)
        }

        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Notifies user of installer outcome.
     */
    private fun notifyStatus(intent: Intent) {
        // unpack from intent
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        val name = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val isUninstall = intent.getBooleanExtra(ACTION_UNINSTALL, false)

        // get application name for notifications
        val appLabel = try {
            if (name != null) {
                packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(
                        name,
                        PackageManager.GET_META_DATA
                    )
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }

        val notificationTag = "download-$name"

        // start building
        val builder = NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_DOWNLOADING)
            .setAutoCancel(true)

        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                if (isUninstall) {
                    // remove any notification for this app
                    notificationManager?.cancel(notificationTag, NOTIFICATION_ID_DOWNLOADING)
                } else {
                    val notification = builder
                        .setSmallIcon(CommonR.drawable.ic_check)
                        .setColor(
                            ContextThemeWrapper(this, CommonR.style.Theme_Main_Light)
                                .getColor(CommonR.color.md_theme_light_primaryContainer)
                        )
                        .setContentTitle("Installed")
                        .setContentText(appLabel)
                        .build()
                    notificationManager?.notify(
                        notificationTag,
                        NOTIFICATION_ID_DOWNLOADING,
                        notification
                    )
                }
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                // do nothing if user cancels
            }

            else -> {
                // problem occurred when installing/uninstalling package
                val notification = builder
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setColor(
                        ContextThemeWrapper(this, CommonR.style.Theme_Main_Light)
                            .getColor(CommonR.color.md_theme_dark_errorContainer)
                    )
                    .setContentTitle("Unknown Error")
                    .setContentText(message)
                    .build()
                notificationManager?.notify(
                    notificationTag,
                    NOTIFICATION_ID_DOWNLOADING,
                    notification
                )
            }
        }
    }
}
