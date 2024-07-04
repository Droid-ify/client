package com.looker.installer.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.looker.core.common.Constants.NOTIFICATION_CHANNEL_INSTALL
import com.looker.core.common.Constants.NOTIFICATION_ID_INSTALL
import com.looker.installer.model.InstallState
import com.looker.installer.model.InstallState.Failed
import com.looker.installer.model.InstallState.Installed
import com.looker.installer.model.InstallState.Installing
import com.looker.installer.model.InstallState.Pending
import com.looker.core.common.R as CommonR

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

fun installTag(name: String): String = "install-${name.trim().replace(' ', '_')}"

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
                setSmallIcon(CommonR.drawable.ic_delete)
                getString(CommonR.string.uninstalled_application) to
                    getString(CommonR.string.uninstalled_application_DESC, appName)
            } else {
                when (state) {
                    Failed -> {
                        setSmallIcon(CommonR.drawable.ic_bug_report)
                        getString(CommonR.string.installation_failed) to
                            getString(CommonR.string.installation_failed_DESC, appName)
                    }

                    Pending -> {
                        setSmallIcon(CommonR.drawable.ic_download)
                        getString(CommonR.string.downloaded_FORMAT, appName) to
                            getString(CommonR.string.tap_to_install_DESC)
                    }

                    Installing -> {
                        setSmallIcon(CommonR.drawable.ic_download)
                        setProgress(-1, -1, true)
                        getString(CommonR.string.installing) to
                            appName
                    }

                    Installed -> {
                        setTimeoutAfter(SUCCESS_TIMEOUT)
                        setSmallIcon(CommonR.drawable.ic_check)
                        getString(CommonR.string.installed) to
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
