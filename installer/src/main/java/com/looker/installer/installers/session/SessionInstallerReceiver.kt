package com.looker.installer.installers.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.looker.core.common.Constants.NOTIFICATION_CHANNEL_INSTALL
import com.looker.core.common.Constants.NOTIFICATION_ID_DOWNLOADING
import com.looker.core.common.Constants.NOTIFICATION_ID_INSTALL
import com.looker.core.common.R
import com.looker.core.common.createNotificationChannel
import com.looker.core.common.extension.getPackageName
import com.looker.core.common.extension.notificationManager

class SessionInstallerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            // prompts user to enable unknown source
            val promptIntent: Intent? = intent.getParcelableExtra(Intent.EXTRA_INTENT)

            promptIntent?.let {
                it.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                it.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                context.startActivity(it)
            }
        } else {
            notifyStatus(intent, context)
        }
    }

    private fun notifyStatus(intent: Intent, context: Context) {
        val packageManager = context.packageManager
        val notificationManager = context.notificationManager

        context.createNotificationChannel(
            id = NOTIFICATION_CHANNEL_INSTALL,
            name = context.getString(R.string.install)
        )

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val isUninstall = intent.getBooleanExtra(ACTION_UNINSTALL, false)

        val appName = packageManager.getPackageName(packageName)

        val notificationTag = "download-$packageName"

        val builder = NotificationCompat
            .Builder(context, NOTIFICATION_CHANNEL_INSTALL)
            .setAutoCancel(true)

        when(status) {
            PackageInstaller.STATUS_SUCCESS -> {
                if (isUninstall) {
                    // remove any notification for this app
                    notificationManager?.cancel(notificationTag, NOTIFICATION_ID_INSTALL)
                } else {
                    val notification = builder
                        .setSmallIcon(R.drawable.ic_check)
                        .setColor(Color.GREEN)
                        .setContentTitle("Installed")
                        .setTimeoutAfter(5_000)
                        .setContentText(appName)
                        .build()
                    notificationManager?.notify(
                        notificationTag,
                        NOTIFICATION_ID_INSTALL,
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
                    .setColor(Color.GREEN)
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

    companion object {
        private const val ACTION_UNINSTALL = "action_uninstall"
    }
}
