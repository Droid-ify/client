package com.looker.droidify.installer.installers.dhizuku

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.Keep
import com.looker.droidify.BuildConfig
import com.looker.droidify.utility.common.log
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Implementation of [IDhizukuInstallerService] that Dhizuku instantiates inside its own
 * device-owner process. Because this code runs with device-owner privileges, the regular
 * [PackageInstaller] installs silently — no hidden APIs are required.
 *
 * Dhizuku requires the bound class to expose a [Keep]-annotated `(Context)` constructor; the
 * [Context] handed in belongs to the device-owner process.
 *
 * **Result delivery:** the Dhizuku server runs us in a *phantom* `app_process` (a child process not
 * registered with ActivityManager), so AMS cannot deliver broadcasts here — a `BroadcastReceiver`
 * for the commit status never fires. Instead we read the outcome from [PackageInstaller.SessionCallback],
 * which is delivered over binder and therefore does reach a phantom process. (commit() still
 * requires a status IntentSender, so we pass a no-op one.) Uninstall has no SessionCallback, so we
 * poll the package state.
 */
@Keep
class DhizukuInstallerService(private val context: Context) : IDhizukuInstallerService.Stub() {

    init {
        logD("service: instantiated pid=${android.os.Process.myPid()} uid=${android.os.Process.myUid()}")
    }

    private val packageInstaller get() = context.packageManager.packageInstaller

    override fun install(
        apk: ParcelFileDescriptor,
        size: Long,
        installerPackageName: String?,
    ): Int {
        return try {
            val params = SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }
            val sessionId = packageInstaller.createSession(params)
            logD("service.install: session=$sessionId size=$size")
            packageInstaller.openSession(sessionId).use { session ->
                ParcelFileDescriptor.AutoCloseInputStream(apk).use { input ->
                    session.openWrite("base.apk", 0, size).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                val status = commitAndAwait(session, sessionId)
                logD("service.install: session=$sessionId status=$status")
                status
            }
        } catch (e: Exception) {
            logD("service.install threw\n${Log.getStackTraceString(e)}", Log.ERROR)
            throw e
        }
    }

    override fun uninstall(packageName: String): Int {
        return try {
            packageInstaller.uninstall(packageName, noOpStatusReceiver())
            // No SessionCallback exists for uninstall and we can't receive the status broadcast in a
            // phantom process, so confirm by polling the package state.
            val removed = awaitPackageRemoved(packageName)
            val status = if (removed) PackageInstaller.STATUS_SUCCESS else PackageInstaller.STATUS_FAILURE
            logD("service.uninstall: $packageName removed=$removed")
            status
        } catch (e: Exception) {
            logD("service.uninstall threw\n${Log.getStackTraceString(e)}", Log.ERROR)
            throw e
        }
    }

    /**
     * Commits [session] and blocks until [PackageInstaller.SessionCallback.onFinished] reports the
     * outcome (binder-delivered, so it works in this phantom process). Returns a
     * `PackageInstaller.STATUS_*` value.
     */
    private fun commitAndAwait(session: PackageInstaller.Session, sessionId: Int): Int {
        val results = ArrayBlockingQueue<Boolean>(1)
        val dispatchThread = HandlerThread("dhizuku-install-result").apply { start() }
        val handler = Handler(dispatchThread.looper)
        val callback = object : PackageInstaller.SessionCallback() {
            override fun onCreated(id: Int) = Unit
            override fun onBadgingChanged(id: Int) = Unit
            override fun onActiveChanged(id: Int, active: Boolean) = Unit
            override fun onProgressChanged(id: Int, progress: Float) = Unit
            override fun onFinished(id: Int, success: Boolean) {
                if (id == sessionId) results.offer(success)
            }
        }
        packageInstaller.registerSessionCallback(callback, handler)
        return try {
            session.commit(noOpStatusReceiver())
            val success = results.poll(TIMEOUT_MINUTES, TimeUnit.MINUTES) ?: false
            if (success) PackageInstaller.STATUS_SUCCESS else PackageInstaller.STATUS_FAILURE
        } finally {
            runCatching { packageInstaller.unregisterSessionCallback(callback) }
            dispatchThread.quitSafely()
        }
    }

    private fun awaitPackageRemoved(packageName: String): Boolean {
        repeat(UNINSTALL_POLLS) {
            if (!isInstalled(packageName)) return true
            Thread.sleep(UNINSTALL_POLL_INTERVAL_MS)
        }
        return !isInstalled(packageName)
    }

    private fun isInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(packageName, 0)
    }.isSuccess

    /**
     * commit()/uninstall() require a status IntentSender, but we read the result from the
     * SessionCallback / package state — the broadcast this fires is never received (phantom process).
     */
    private fun noOpStatusReceiver(): IntentSender {
        // commit()/uninstall() reject immutable senders (the system fills in status extras), so this
        // must be MUTABLE even though we never read the resulting broadcast.
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val intent = Intent(NOOP_ACTION).setPackage(context.packageName)
        return PendingIntent.getBroadcast(context, 0, intent, flags).intentSender
    }

    // Debug-only: stripped (no-op) in release builds.
    private fun logD(message: String, type: Int = Log.INFO) {
        if (BuildConfig.DEBUG) log(message, TAG, type)
    }

    private companion object {
        const val TIMEOUT_MINUTES = 3L
        const val UNINSTALL_POLLS = 20
        const val UNINSTALL_POLL_INTERVAL_MS = 250L
        const val NOOP_ACTION = "com.looker.droidify.DHIZUKU_STATUS_NOOP"
        const val TAG = "DroidifyDhizuku"
    }
}
