package com.looker.droidify.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_UNARCHIVE_PACKAGE
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_ALL_USERS
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_ID
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_PACKAGE_NAME
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.looker.droidify.work.UnarchiveWorker
import dagger.hilt.android.AndroidEntryPoint

private val TAG = UnarchivePackageReceiver::class.java.simpleName

// Port from https://gitlab.com/fdroid/fdroidclient/-/merge_requests/1436

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
class UnarchivePackageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_UNARCHIVE_PACKAGE) {
            Log.w(TAG, "Unknown action: ${intent.action}")
            return
        }
        val packageName = intent.getStringExtra(EXTRA_UNARCHIVE_PACKAGE_NAME) ?: error("")
        val unarchiveId = intent.getIntExtra(EXTRA_UNARCHIVE_ID, -1)
        val allUsers = intent.getBooleanExtra(EXTRA_UNARCHIVE_ALL_USERS, false)

        Log.i(TAG, "Intent received, un-archiving $packageName...")

        UnarchiveWorker.updateNow(context, packageName, unarchiveId, allUsers)
    }
}
