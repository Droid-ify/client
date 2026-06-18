package com.looker.droidify.installer.installers.dhizuku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.looker.droidify.utility.common.extension.getLauncherActivities
import com.looker.droidify.utility.common.extension.intent
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val DHIZUKU_PACKAGE = "com.rosan.dhizuku"

fun isDhizukuInstalled(context: Context): Boolean =
    try {
        context.packageManager.getPackageInfo(DHIZUKU_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

/**
 * True when a Dhizuku-compatible server is reachable. [Dhizuku.init] resolves the server from the
 * active device owner's provider rather than a fixed package, so this also matches third-party
 * servers such as OwnDroid's built-in Dhizuku server — not only the standalone Dhizuku app. Gating
 * this on [isDhizukuInstalled] (a hardcoded `com.rosan.dhizuku` lookup) would reject those servers
 * even when they are running, so the install check is intentionally left out here.
 */
fun isDhizukuAlive(context: Context): Boolean =
    try {
        Dhizuku.init(context)
    } catch (_: Exception) {
        false
    }

fun isDhizukuGranted(): Boolean =
    try {
        Dhizuku.isPermissionGranted()
    } catch (_: Exception) {
        false
    }

fun launchDhizuku(context: Context) {
    if (!isDhizukuInstalled(context)) return
    val activities = context.packageManager.getLauncherActivities(DHIZUKU_PACKAGE)
    if (activities.isEmpty()) return
    val launchIntent = intent(Intent.ACTION_MAIN) {
        addCategory(Intent.CATEGORY_LAUNCHER)
        setComponent(
            ComponentName(
                DHIZUKU_PACKAGE,
                activities.first().first,
            ),
        )
        setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(launchIntent)
}

private const val DHIZUKU_WAKE_POLL_ATTEMPTS = 15
private const val DHIZUKU_WAKE_POLL_DELAY_MS = 200L
private const val DHIZUKU_INSTALLER_READY_ATTEMPTS = 25
private const val DHIZUKU_INSTALLER_STABILITY_DELAY_MS = 200L

suspend fun ensureDhizukuAlive(context: Context): Boolean {
    if (isDhizukuAlive(context)) return true
    if (!isDhizukuInstalled(context)) return false
    launchDhizuku(context)
    repeat(DHIZUKU_WAKE_POLL_ATTEMPTS) {
        delay(DHIZUKU_WAKE_POLL_DELAY_MS)
        if (isDhizukuAlive(context)) return true
    }
    return false
}

/** Dhizuku server may throw while Koin is still starting after wake; poll until stable. */
suspend fun ensureDhizukuGranted(): Boolean {
    repeat(DHIZUKU_WAKE_POLL_ATTEMPTS) {
        if (isDhizukuGranted()) return true
        delay(DHIZUKU_WAKE_POLL_DELAY_MS)
    }
    return isDhizukuGranted()
}

/**
 * Wake Dhizuku and wait until permission + DI are stable enough for install/uninstall RPC.
 * Needed for update-all and other background installs where the UI does not prepare Dhizuku first.
 */
suspend fun ensureDhizukuInstallerReady(context: Context): Boolean {
    if (!ensureDhizukuAlive(context)) return false
    if (!ensureDhizukuGranted()) return false
    repeat(DHIZUKU_INSTALLER_READY_ATTEMPTS) {
        delay(DHIZUKU_INSTALLER_STABILITY_DELAY_MS)
        if (!isDhizukuAlive(context)) {
            ensureDhizukuAlive(context)
            return@repeat
        }
        if (isDhizukuGranted()) return true
    }
    return isDhizukuAlive(context) && isDhizukuGranted()
}

suspend fun requestDhizukuPermission(context: Context): Boolean =
    suspendCancellableCoroutine { continuation ->
        if (!Dhizuku.init(context)) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        if (isDhizukuGranted()) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }
        Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
            override fun onRequestPermission(grantResult: Int) {
                if (continuation.isActive) {
                    continuation.resume(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
        })
    }
