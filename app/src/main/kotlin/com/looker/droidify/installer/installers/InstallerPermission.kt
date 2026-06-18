package com.looker.droidify.installer.installers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.looker.droidify.BuildConfig
import com.looker.droidify.utility.common.SdkCheck
import com.looker.droidify.utility.common.extension.getLauncherActivities
import com.looker.droidify.utility.common.extension.getPackageInfoCompat
import com.looker.droidify.utility.common.extension.intent
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import com.rosan.dhizuku.shared.DhizukuVariables
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import rikka.sui.Sui
import kotlin.coroutines.resume

private const val SHIZUKU_PERMISSION_REQUEST_CODE = 87263

internal const val DHIZUKU_LOG_TAG = "DroidifyDhizuku"

// Debug-only: stripped (no-op) in release builds.
internal fun logDhizuku(message: String, type: Int = Log.INFO) {
    if (BuildConfig.DEBUG) Log.println(type, DHIZUKU_LOG_TAG, message)
}

/**
 * Gracefully unfreezes the Dhizuku server process (e.g. OwnDroid, which runs no foreground service
 * and gets frozen when backgrounded). A *real* ContentProvider transaction is AMS-mediated and
 * unfreezes the target, unlike acquiring the client alone or a raw binder call — the latter just
 * returns "error -74 (sent to frozen apps)". Best-effort; callers still retry as a safety net.
 */
fun wakeDhizukuServer(context: Context) {
    if (!SdkCheck.isOreo) return
    runCatching {
        val authority = DhizukuVariables.getProviderAuthorityName(Dhizuku.getOwnerPackageName())
        val uri = Uri.parse("content://$authority")
        context.contentResolver.acquireUnstableContentProviderClient(uri)?.use { client ->
            // Result is irrelevant — delivering the call is what forces the unfreeze.
            runCatching { client.call("ping", null, null) }
        }
    }.onFailure { logDhizuku("wakeDhizukuServer failed: ${it.message}", Log.WARN) }
}

fun launchShizuku(context: Context) {
    val packageName = context.shizukuPackageName()
        ?: ShizukuProvider.MANAGER_APPLICATION_ID
    val activities = context.packageManager.getLauncherActivities(packageName)
    if (activities.isEmpty()) return
    val intent = intent(Intent.ACTION_MAIN) {
        addCategory(Intent.CATEGORY_LAUNCHER)
        setComponent(
            ComponentName(
                packageName,
                activities.first().first,
            ),
        )
        setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

fun initSui(context: Context) = Sui.init(context.packageName)

fun isSuiAvailable() = Sui.isSui()

private fun Context.shizukuPermissionInfo() =
    runCatching {
        packageManager.getPermissionInfo(ShizukuProvider.PERMISSION, 0)
    }.getOrNull()

private fun Context.shizukuPackageName() = shizukuPermissionInfo()?.packageName

fun isShizukuInstalled(context: Context) =
    context.shizukuPermissionInfo() != null ||
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

/**
 * True when a Dhizuku-compatible server is present and bindable. [Dhizuku.init] discovers the
 * server by intent action rather than a fixed package, so this also matches third-party servers
 * such as OwnDroid's built-in Dhizuku server — not just the standalone Dhizuku app.
 */
fun isDhizukuAlive(context: Context): Boolean {
    if (!SdkCheck.isOreo) return false
    wakeDhizukuServer(context)
    val alive = runCatching { Dhizuku.init(context) }
        .onFailure { logDhizuku("Dhizuku.init threw: ${it.message}", Log.WARN) }
        .getOrDefault(false)
    logDhizuku("isDhizukuAlive=$alive")
    return alive
}

/**
 * The Dhizuku server is often a frozen/cached process (e.g. OwnDroid has no foreground service), so
 * the first contact can fail — binder calls to a frozen app return "error -74" and `Dhizuku.init`
 * reports unavailable until the process is woken. [wakeDhizukuServer] (run inside [isDhizukuAlive])
 * unfreezes it via a provider transaction; poll a few times to ride out the wake window.
 */
suspend fun awaitDhizukuAlive(
    context: Context,
    attempts: Int = 3,
    delayMs: Long = 300,
): Boolean {
    repeat(attempts) { attempt ->
        if (isDhizukuAlive(context)) return true
        logDhizuku("awaitDhizukuAlive: attempt ${attempt + 1}/$attempts not ready", Log.WARN)
        if (attempt < attempts - 1) delay(delayMs)
    }
    return false
}

fun isDhizukuGranted() =
    runCatching { Dhizuku.isPermissionGranted() }.getOrDefault(false)

suspend fun requestDhizukuPermission() = suspendCancellableCoroutine {
    Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
        override fun onRequestPermission(grantResult: Int) {
            it.resume(grantResult == PackageManager.PERMISSION_GRANTED)
        }
    })
}

fun isMagiskGranted(): Boolean {
    com.topjohnwu.superuser.Shell.getCachedShell() ?: com.topjohnwu.superuser.Shell.getShell()
    return com.topjohnwu.superuser.Shell.isAppGrantedRoot() == true
}
