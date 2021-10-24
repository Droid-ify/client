package com.looker.droidify.installer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import com.looker.droidify.utility.extension.android.Android
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job


abstract class BaseInstaller(val context: Context) : InstallationEvents {

    companion object {
        const val ROOT_INSTALL_PACKAGE = "cat %s | pm install --user %s -t -r -S %s"
        const val ROOT_UNINSTALL_PACKAGE = "pm uninstall --user %s %s"
    }

    private val job = Job()
    val scope = CoroutineScope(Dispatchers.IO + job)

    fun getStatusString(context: Context, status: Int): String {
        return when (status) {
            PackageInstaller.STATUS_FAILURE -> "context.getString(R.string.installer_status_failure)"
            PackageInstaller.STATUS_FAILURE_ABORTED -> "context.getString(R.string.installer_status_failure_aborted)"
            PackageInstaller.STATUS_FAILURE_BLOCKED -> "context.getString(R.string.installer_status_failure_blocked)"
            PackageInstaller.STATUS_FAILURE_CONFLICT -> "context.getString(R.string.installer_status_failure_conflict)"
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "context.getString(R.string.installer_status_failure_incompatible)"
            PackageInstaller.STATUS_FAILURE_INVALID -> "context.getString(R.string.installer_status_failure_invalid)"
            PackageInstaller.STATUS_FAILURE_STORAGE -> "context.getString(R.string.installer_status_failure_storage)"
            PackageInstaller.STATUS_PENDING_USER_ACTION -> "context.getString(R.string.installer_status_user_action)"
            PackageInstaller.STATUS_SUCCESS -> "context.getString(R.string.installer_status_success)"
            else -> "context.getString(R.string.installer_status_unknown)"
        }
    }

    override fun uninstall(packageName: String) {
        val uri = Uri.fromParts("package", packageName, null)
        val intent = Intent()
        intent.data = uri
        if (Android.sdk(28)) {
            intent.action = Intent.ACTION_DELETE
        } else {
            intent.action = Intent.ACTION_UNINSTALL_PACKAGE
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    sealed class Event {
        object INSTALL : Event()
        object UNINSTALL : Event()
    }
}