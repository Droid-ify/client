package com.looker.droidify.utility.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.looker.droidify.R
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.utility.common.Constants.NOTIFICATION_CHANNEL_INSTALL
import com.looker.droidify.utility.common.Constants.NOTIFICATION_ID_INSTALL

fun NotificationManager.installNotification(
    packageName: String,
    notification: Notification,
) {
    notify(
        installTag(packageName),
        NOTIFICATION_ID_INSTALL,
        notification
    )
}

fun NotificationManager.removeInstallNotification(
    packageName: String,
) {
    cancel(installTag(packageName), NOTIFICATION_ID_INSTALL)
}

private fun installTag(name: String): String = "install-${name.trim().replace(' ', '_')}"

private const val SUCCESS_TIMEOUT = 5_000L

fun Context.createInstallNotification(
    appName: String,
    state: InstallState,
    isUninstall: Boolean = false,
    autoCancel: Boolean = true,
    block: NotificationCompat.Builder.() -> Unit = {},
): Notification {
    return NotificationCompat
        .Builder(this, NOTIFICATION_CHANNEL_INSTALL)
        .apply {
            setAutoCancel(autoCancel)
            setOngoing(false)
            setOnlyAlertOnce(true)
            setColor(Color.GREEN)
            val (title, text) = if (isUninstall) {
                setTimeoutAfter(SUCCESS_TIMEOUT)
                setSmallIcon(R.drawable.ic_delete)
                getString(R.string.uninstalled_application) to
                    getString(R.string.uninstalled_application_DESC, appName)
            } else {
                when (state) {
                    InstallState.Failed -> {
                        setSmallIcon(R.drawable.ic_bug_report)
                        getString(R.string.installation_failed) to
                            getString(R.string.installation_failed_DESC, appName)
                    }

                    InstallState.Pending -> {
                        setSmallIcon(R.drawable.ic_download)
                        getString(R.string.downloaded_FORMAT, appName) to
                            getString(R.string.tap_to_install_DESC)
                    }

                    InstallState.Installing -> {
                        setSmallIcon(R.drawable.ic_download)
                        setProgress(-1, -1, true)
                        getString(R.string.installing) to
                            appName
                    }

                    InstallState.Installed -> {
                        setTimeoutAfter(SUCCESS_TIMEOUT)
                        setSmallIcon(R.drawable.ic_check)
                        getString(R.string.installed) to
                            appName
                    }
                }
            }
            setContentTitle(title)
            setContentText(text)
            block()
        }
        .build()
}
