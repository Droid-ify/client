package com.looker.droidify.installer.installers.dhizuku

import android.content.Context
import android.content.pm.PackageManager
import com.looker.droidify.utility.common.extension.getPackageInfoCompat
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

fun isDhizukuInstalled(context: Context): Boolean =
    runCatching {
        context.packageManager.getPermissionInfo("com.rosan.dhizuku.permission.API", 0)
    }.getOrNull() != null ||
        context.packageManager.getPackageInfoCompat("com.rosan.dhizuku") != null

suspend fun isDhizukuAvailable(context: Context): Boolean =
    withContext(Dispatchers.IO) { Dhizuku.init(context) }

fun isDhizukuGranted(): Boolean = Dhizuku.isPermissionGranted()

suspend fun requestDhizukuPermission(): Boolean = suspendCancellableCoroutine { cont ->
    Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
        override fun onRequestPermission(grantResult: Int) {
            if (cont.isActive) cont.resume(grantResult == PackageManager.PERMISSION_GRANTED)
        }
    })
}
