package com.looker.installer.installers.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.looker.core.common.Constants.NOTIFICATION_CHANNEL_INSTALL
import com.looker.core.common.R
import com.looker.core.common.createNotificationChannel
import com.looker.core.common.extension.getPackageName
import com.looker.core.common.extension.notificationManager
import com.looker.core.domain.model.toPackageName
import com.looker.installer.InstallManager
import com.looker.installer.model.InstallState
import com.looker.installer.notification.createInstallNotification
import com.looker.installer.notification.installNotification
import com.looker.installer.notification.removeInstallNotification
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SessionInstallerReceiver : BroadcastReceiver() {

    // This is a cyclic dependency injection, I know but this is the best option for now
    @Inject
    lateinit var installManager: InstallManager

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

        if (packageName != null) {
            when (status) {
                PackageInstaller.STATUS_SUCCESS -> {
                    notificationManager?.removeInstallNotification(packageName)
                    val notification = context.createInstallNotification(
                        appName = (appName ?: packageName.substringAfterLast('.')).toString(),
                        state = InstallState.Installed,
                        isUninstall = isUninstall,
                    ) {
                        setTimeoutAfter(SUCCESS_TIMEOUT)
                    }
                    notificationManager?.installNotification(
                        packageName = packageName.toString(),
                        notification = notification,
                    )
                }

                PackageInstaller.STATUS_FAILURE_ABORTED -> {
                    installManager.remove(packageName.toPackageName())
                }

                else -> {
                    installManager.remove(packageName.toPackageName())
                    val notification = context.createInstallNotification(
                        appName = appName.toString(),
                        state = InstallState.Failed,
                    ) {
                        setContentText(message)
                    }
                    notificationManager?.installNotification(
                        packageName = packageName,
                        notification = notification
                    )
                }
            }
        }
    }

    companion object {
        const val ACTION_UNINSTALL = "action_uninstall"

        private const val SUCCESS_TIMEOUT = 5_000L
    }
}
