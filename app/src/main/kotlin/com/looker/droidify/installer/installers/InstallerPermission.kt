package com.looker.droidify.installer.installers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.looker.droidify.utility.common.extension.getLauncherActivities
import com.looker.droidify.utility.common.extension.getPackageInfoCompat
import com.looker.droidify.utility.common.extension.intent
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import rikka.sui.Sui
import kotlin.coroutines.resume

private const val SHIZUKU_PERMISSION_REQUEST_CODE = 87263

fun launchShizuku(context: Context) {
    val activities =
        context.packageManager.getLauncherActivities(ShizukuProvider.MANAGER_APPLICATION_ID)
    val intent = intent(Intent.ACTION_MAIN) {
        addCategory(Intent.CATEGORY_LAUNCHER)
        setComponent(
            ComponentName(
                ShizukuProvider.MANAGER_APPLICATION_ID,
                activities.first().first,
            ),
        )
        setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

fun initSui(context: Context) = Sui.init(context.packageName)

fun isSuiAvailable() = Sui.isSui()

fun isShizukuInstalled(context: Context) =
    context.packageManager.getPackageInfoCompat(ShizukuProvider.MANAGER_APPLICATION_ID) != null

fun isShizukuAlive() = Shizuku.pingBinder()

fun isShizukuGranted() = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

suspend fun requestPermissionListener() = suspendCancellableCoroutine {
    val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            it.resume(grantResult == PackageManager.PERMISSION_GRANTED)
        }
    }
    Shizuku.addRequestPermissionResultListener(listener)
    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
    it.invokeOnCancellation {
        Shizuku.removeRequestPermissionResultListener(listener)
    }
}

fun requestShizuku() {
    Shizuku.shouldShowRequestPermissionRationale()
    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
}

fun isMagiskGranted(): Boolean {
    com.topjohnwu.superuser.Shell.getCachedShell() ?: com.topjohnwu.superuser.Shell.getShell()
    return com.topjohnwu.superuser.Shell.isAppGrantedRoot() == true
}
