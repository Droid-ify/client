package com.looker.droidify.installer.installers.dhizuku

import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.util.Log
import androidx.annotation.Keep
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Keep
class DroidifyDhizukuInstallerService @JvmOverloads constructor(
    private val serviceContext: Context? = null,
) : IDhizukuInstallerService.Stub() {

    companion object {
        const val STATUS_SUCCESS = 0
        const val STATUS_FAILURE = -1
        const val STATUS_PENDING_USER_ACTION_REQUIRED = -2

        private const val TAG = "DroidifyDhizuku"
        private const val INSTALL_TIMEOUT_SECONDS = 120L
        private const val UNINSTALL_TIMEOUT_SECONDS = 30L
        private const val INSTALL_POLL_INTERVAL_MS = 200L
        private const val UNINSTALL_POLL_INTERVAL_MS = 200L
        private const val ACTION_INSTALL_RESULT = "com.looker.droidify.dhizuku.INSTALL_RESULT"
        private const val ACTION_UNINSTALL_RESULT = "com.looker.droidify.dhizuku.UNINSTALL_RESULT"
    }

    override fun installPackage(
        pfd: ParcelFileDescriptor,
        fileSize: Long,
        expectedPackageName: String?,
        expectedVersionCode: Long,
        installerPackageName: String?,
    ): Int {
        Log.d(
            TAG,
            "installPackage size=$fileSize expected=$expectedPackageName@$expectedVersionCode " +
                "installer=$installerPackageName uid=${android.os.Process.myUid()}",
        )

        val ctx = resolveContext() ?: run {
            Log.e(TAG, "No application context in Dhizuku process")
            throw RemoteException("No application context in Dhizuku process")
        }

        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        if (fileSize > 0) {
            params.setSize(fileSize)
        }
        installerPackageName?.takeIf { it.isNotBlank() }?.let { name ->
            try {
                params.setInstallerPackageName(name)
            } catch (e: Exception) {
                Log.w(TAG, "setInstallerPackageName($name) failed: ${e.message}")
            }
        }

        var sessionId = -1
        var session: PackageInstaller.Session? = null
        var receiver: BroadcastReceiver? = null
        var committed = false
        var installError: String? = null
        return try {
            sessionId = installer.createSession(params)
            Log.d(TAG, "createSession id=$sessionId pkg=${ctx.packageName}")
            session = installer.openSession(sessionId)

            session.openWrite("apk", 0, fileSize).use { out ->
                ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                    val copied = input.copyTo(out)
                    out.flush()
                    session.fsync(out)
                    Log.d(TAG, "streamed $copied bytes to session")
                }
            }

            val latch = CountDownLatch(1)
            val resultRef = AtomicInteger(STATUS_FAILURE)
            val token = UUID.randomUUID().toString()
            val action = "$ACTION_INSTALL_RESULT.$token"

            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val status = intent.getIntExtra(
                        PackageInstaller.EXTRA_STATUS,
                        PackageInstaller.STATUS_FAILURE,
                    )
                    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Log.d(TAG, "install callback status=$status message=$message")
                    when (status) {
                        PackageInstaller.STATUS_SUCCESS -> resultRef.set(STATUS_SUCCESS)
                        PackageInstaller.STATUS_PENDING_USER_ACTION ->
                            resultRef.set(STATUS_PENDING_USER_ACTION_REQUIRED)
                        else -> {
                            installError = message ?: "PackageInstaller status=$status"
                            resultRef.set(STATUS_FAILURE)
                        }
                    }
                    latch.countDown()
                }
            }
            registerInternalReceiver(ctx, receiver, action)

            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                ctx,
                token.hashCode(),
                Intent(action).setPackage(ctx.packageName),
                pendingFlags,
            )

            session.commit(pendingIntent.intentSender)
            committed = true

            val deadlineNanos = System.nanoTime() +
                TimeUnit.SECONDS.toNanos(INSTALL_TIMEOUT_SECONDS)
            var result = STATUS_FAILURE
            while (System.nanoTime() < deadlineNanos) {
                val remainingNanos = deadlineNanos - System.nanoTime()
                if (remainingNanos <= 0L) break
                val waitMs = minOf(
                    INSTALL_POLL_INTERVAL_MS,
                    TimeUnit.NANOSECONDS.toMillis(remainingNanos).coerceAtLeast(1L),
                )
                if (latch.await(waitMs, TimeUnit.MILLISECONDS)) {
                    result = resultRef.get()
                    break
                }
                if (verifyInstallSucceeded(
                        ctx,
                        installer,
                        sessionId,
                        expectedPackageName,
                        expectedVersionCode,
                    )
                ) {
                    Log.d(TAG, "install detected via package presence: $expectedPackageName")
                    result = STATUS_SUCCESS
                    break
                }
            }
            if (result == STATUS_FAILURE) {
                result = if (verifyInstallSucceeded(
                        ctx,
                        installer,
                        sessionId,
                        expectedPackageName,
                        expectedVersionCode,
                    )
                ) {
                    Log.d(TAG, "install succeeded after timeout (package present): $expectedPackageName")
                    STATUS_SUCCESS
                } else if (resultRef.get() != STATUS_FAILURE) {
                    resultRef.get()
                } else {
                    installError = "Install timed out"
                    STATUS_FAILURE
                }
            }
            if (result != STATUS_SUCCESS && result != STATUS_PENDING_USER_ACTION_REQUIRED) {
                throw RemoteException(installError ?: "Install failed with code $result")
            }
            result
        } catch (e: RemoteException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "installPackage exception", e)
            throw RemoteException(e.message ?: e.javaClass.simpleName)
        } finally {
            if (!committed && sessionId >= 0) {
                try {
                    installer.abandonSession(sessionId)
                } catch (_: Exception) {
                }
            }
            try {
                session?.close()
            } catch (_: Exception) {
            }
            if (receiver != null) {
                try {
                    ctx.unregisterReceiver(receiver)
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun uninstallPackage(packageName: String?): Int {
        if (packageName.isNullOrBlank()) {
            return STATUS_SUCCESS
        }

        Log.d(TAG, "uninstallPackage: $packageName uid=${android.os.Process.myUid()}")

        val ctx = resolveContext() ?: return STATUS_FAILURE
        val installer = ctx.packageManager.packageInstaller
        val latch = CountDownLatch(1)
        val resultRef = AtomicInteger(STATUS_FAILURE)
        val token = UUID.randomUUID().toString()
        val action = "$ACTION_UNINSTALL_RESULT.$token"
        var receiver: BroadcastReceiver? = null

        return try {
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val status = intent.getIntExtra(
                        PackageInstaller.EXTRA_STATUS,
                        PackageInstaller.STATUS_FAILURE,
                    )
                    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    Log.d(TAG, "uninstall callback status=$status message=$message pkg=$packageName")
                    resultRef.set(
                        if (status == PackageInstaller.STATUS_SUCCESS) STATUS_SUCCESS else STATUS_FAILURE,
                    )
                    latch.countDown()
                }
            }
            registerInternalReceiver(ctx, receiver, action)

            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                ctx,
                token.hashCode(),
                Intent(action).setPackage(ctx.packageName),
                pendingFlags,
            )

            installer.uninstall(packageName, pendingIntent.intentSender)
            val deadlineNanos = System.nanoTime() +
                TimeUnit.SECONDS.toNanos(UNINSTALL_TIMEOUT_SECONDS)
            while (System.nanoTime() < deadlineNanos) {
                val remainingNanos = deadlineNanos - System.nanoTime()
                if (remainingNanos <= 0L) break
                val waitMs = minOf(
                    UNINSTALL_POLL_INTERVAL_MS,
                    TimeUnit.NANOSECONDS.toMillis(remainingNanos).coerceAtLeast(1L),
                )
                if (latch.await(waitMs, TimeUnit.MILLISECONDS)) {
                    return resultRef.get()
                }
                if (!isPackageInstalled(ctx, packageName)) {
                    Log.d(TAG, "uninstall detected via package absence: $packageName")
                    return STATUS_SUCCESS
                }
            }
            if (!isPackageInstalled(ctx, packageName)) {
                Log.d(TAG, "uninstall succeeded after timeout (package gone): $packageName")
                STATUS_SUCCESS
            } else {
                resultRef.get()
            }
        } catch (e: Exception) {
            Log.e(TAG, "uninstallPackage exception", e)
            STATUS_FAILURE
        } finally {
            if (receiver != null) {
                try {
                    ctx.unregisterReceiver(receiver)
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun destroy() {}

    private fun resolveContext(): Context? {
        serviceContext?.let { ctx ->
            return ctx.applicationContext ?: ctx
        }
        // No service Context was supplied: accept the host process only if it is the active device
        // owner. Dhizuku loads this service into the device-owner process regardless of which app
        // provides the server (the standalone Dhizuku app, OwnDroid's built-in server, ...), so
        // verify ownership instead of matching a hardcoded package name.
        val app = currentApplicationOrNull() ?: return null
        return if (isDeviceOwner(app)) app.applicationContext ?: app else null
    }

    private fun verifyInstallSucceeded(
        ctx: Context,
        installer: PackageInstaller,
        sessionId: Int,
        expectedPackageName: String?,
        expectedVersionCode: Long,
    ): Boolean {
        val targetPackage = expectedPackageName?.takeIf { it.isNotBlank() }
            ?: runCatching { installer.getSessionInfo(sessionId)?.appPackageName }
                .onFailure { Log.e(TAG, "getSessionInfo() failed during verification", it) }
                .getOrNull()
            ?: return false
        val info = packageInfoOrNull(ctx, targetPackage) ?: return false
        if (expectedVersionCode <= 0L) return true
        val installedVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        return installedVersion >= expectedVersionCode
    }

    private fun isDeviceOwner(ctx: Context): Boolean = try {
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        dpm?.isDeviceOwnerApp(ctx.packageName) == true
    } catch (e: Exception) {
        Log.w(TAG, "Device-owner check failed", e)
        false
    }

    private fun isPackageInstalled(ctx: Context, packageName: String): Boolean =
        packageInfoOrNull(ctx, packageName) != null

    private fun packageInfoOrNull(ctx: Context, packageName: String) = try {
        ctx.packageManager.getPackageInfo(packageName, 0)
    } catch (_: Exception) {
        null
    }

    private fun currentApplicationOrNull(): Context? = try {
        val cls = Class.forName("android.app.ActivityThread")
        cls.getMethod("currentApplication").invoke(null) as? Context
    } catch (e: Exception) {
        try {
            val cls = Class.forName("android.app.AppGlobals")
            cls.getMethod("getInitialApplication").invoke(null) as? Context
        } catch (_: Exception) {
            Log.e(TAG, "Failed to obtain Application context", e)
            null
        }
    }

    private fun registerInternalReceiver(ctx: Context, receiver: BroadcastReceiver, action: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(receiver, IntentFilter(action), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            ctx.registerReceiver(receiver, IntentFilter(action))
        }
    }
}
