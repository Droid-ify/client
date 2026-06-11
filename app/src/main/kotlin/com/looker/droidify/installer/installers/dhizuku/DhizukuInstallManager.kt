package com.looker.droidify.installer.installers.dhizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.DeadObjectException
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.util.Log
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuUserServiceArgs
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DhizukuInstallManager(private val context: Context) {
    private val bindLock = Any()

    @Volatile
    private var installerService: IDhizukuInstallerService? = null

    @Volatile
    private var serviceConnection: ServiceConnection? = null

    fun installApk(apkPath: String) {
        if (!Dhizuku.init(context)) {
            throw IllegalStateException("Dhizuku is not available")
        }
        if (!Dhizuku.isPermissionGranted()) {
            throw SecurityException("Dhizuku permission denied")
        }

        val file = File(apkPath)
        if (!file.exists() || file.length() <= 0L) {
            throw IllegalArgumentException("APK file missing or empty")
        }

        var lastError: Exception? = null
        repeat(MAX_INSTALL_ATTEMPTS) { attempt ->
            if (attempt > 0) {
                Log.w(TAG, "Retrying Dhizuku install after rebinding (attempt ${attempt + 1})")
                synchronized(dhizukuOperationLock) {
                    resetUserServiceState()
                }
            }

            try {
                val service = bindInstallerService(clearCache = attempt == 0)
                    ?: throw IllegalStateException("Failed to bind Dhizuku installer service")
                performInstall(service, file)
                return
            } catch (e: Exception) {
                lastError = e
                if (attempt == MAX_INSTALL_ATTEMPTS - 1 || !shouldRetryBind(e)) {
                    throw e
                }
                if (isDhizukuStartupError(e)) {
                    launchDhizuku(context)
                    Thread.sleep(DHIZUKU_STARTUP_SETTLE_MS)
                }
                Log.w(TAG, "Dhizuku install failed, will rebind: ${e.message}")
            }
        }

        throw lastError ?: IllegalStateException("Dhizuku install failed")
    }

    fun uninstallPackage(packageName: String) {
        if (!Dhizuku.init(context)) {
            throw IllegalStateException("Dhizuku is not available")
        }
        if (!Dhizuku.isPermissionGranted()) {
            throw SecurityException("Dhizuku permission denied")
        }

        var lastError: Exception? = null
        repeat(MAX_INSTALL_ATTEMPTS) { attempt ->
            if (attempt > 0) {
                Log.w(TAG, "Retrying Dhizuku uninstall after rebinding (attempt ${attempt + 1})")
                synchronized(dhizukuOperationLock) {
                    resetUserServiceState()
                }
            }

            try {
                val service = bindInstallerService(clearCache = attempt == 0)
                    ?: throw IllegalStateException("Failed to bind Dhizuku installer service")
                val result = service.uninstallPackage(packageName)
                if (result == DroidifyDhizukuInstallerService.STATUS_SUCCESS) {
                    return
                }
                throw IllegalStateException("Uninstall failed with code $result")
            } catch (e: Exception) {
                lastError = e
                if (attempt == MAX_INSTALL_ATTEMPTS - 1 || !shouldRetryBind(e)) {
                    throw e
                }
                if (isDhizukuStartupError(e)) {
                    launchDhizuku(context)
                    Thread.sleep(DHIZUKU_STARTUP_SETTLE_MS)
                }
                Log.w(TAG, "Dhizuku uninstall failed, will rebind: ${e.message}")
            }
        }

        throw lastError ?: IllegalStateException("Dhizuku uninstall failed")
    }

    /** Bind only — remote install/uninstall RPC runs outside the lock so ops can overlap. */
    private fun bindInstallerService(clearCache: Boolean): IDhizukuInstallerService? =
        synchronized(dhizukuOperationLock) {
            if (clearCache) {
                clearStaleDhizukuClientCache(userServiceArgs())
            }
            getOrBindService()
        }

    private fun performInstall(service: IDhizukuInstallerService, file: File) {
        val (expectedPackageName, expectedVersionCode) = readApkIdentity(file.absolutePath)
        var result = openAndInstall(
            service,
            file,
            expectedPackageName,
            expectedVersionCode,
            null,
        )

        if (result == DroidifyDhizukuInstallerService.STATUS_PENDING_USER_ACTION_REQUIRED) {
            result = openAndInstall(
                service,
                file,
                expectedPackageName,
                expectedVersionCode,
                context.packageName,
            )
        }

        when (result) {
            DroidifyDhizukuInstallerService.STATUS_SUCCESS -> Unit
            DroidifyDhizukuInstallerService.STATUS_PENDING_USER_ACTION_REQUIRED ->
                throw IllegalStateException("Install requires user action")
            else -> throw IllegalStateException("Install failed with code $result")
        }
    }

    private fun openAndInstall(
        service: IDhizukuInstallerService,
        file: File,
        expectedPackageName: String?,
        expectedVersionCode: Long,
        installerPackageName: String?,
    ): Int {
        try {
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                service.installPackage(
                    pfd,
                    file.length(),
                    expectedPackageName,
                    expectedVersionCode,
                    installerPackageName,
                )
            }
        } catch (e: RemoteException) {
            throw IllegalStateException(formatRemoteError(e), e)
        } catch (e: DeadObjectException) {
            throw IllegalStateException("Dhizuku installer service disconnected", e)
        }
    }

    private fun getOrBindService(): IDhizukuInstallerService? {
        synchronized(bindLock) {
            installerService?.let { service ->
                val alive = try {
                    service.asBinder().pingBinder()
                } catch (_: Exception) {
                    false
                }
                if (alive) {
                    return service
                }
                invalidateServiceLocked()
                clearStaleDhizukuClientCache(userServiceArgs())
                stopUserServiceQuietly()
            }
        }
        return bindServiceSync()
    }

    private fun userServiceArgs(): DhizukuUserServiceArgs {
        val componentName = ComponentName(
            context.packageName,
            DroidifyDhizukuInstallerService::class.java.name,
        )
        return DhizukuUserServiceArgs(componentName)
    }

    private fun invalidateServiceLocked() {
        val conn = serviceConnection
        installerService = null
        serviceConnection = null
        if (conn != null) {
            try {
                Dhizuku.unbindUserService(conn)
            } catch (e: Exception) {
                Log.w(TAG, "unbindUserService failed: ${e.message}")
            }
        }
    }

    private fun resetUserServiceState() {
        invalidateServiceLocked()
        clearStaleDhizukuClientCache(userServiceArgs())
        stopUserServiceQuietly()
    }

    private fun stopUserServiceQuietly() {
        try {
            Dhizuku.stopUserService(userServiceArgs())
            Thread.sleep(STOP_SETTLE_MS)
        } catch (e: Exception) {
            Log.w(TAG, "stopUserService failed: ${e.message}")
        }
    }

    private fun clearStaleDhizukuClientCache(args: DhizukuUserServiceArgs) {
        try {
            val clazz = Class.forName("com.rosan.dhizuku.api.DhizukuServiceConnections")
            val servicesField = clazz.getDeclaredField("services")
            servicesField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val services = servicesField.get(null) as MutableMap<String, IBinder>
            val token = args.componentName.flattenToString()
            if (services.remove(token) != null) {
                Log.d(TAG, "Cleared stale Dhizuku client binder cache for $token")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear Dhizuku client cache: ${e.message}")
        }
    }

    private fun isBinderAlive(binder: IBinder?): Boolean {
        if (binder == null) return false
        return try {
            binder.pingBinder()
        } catch (_: Exception) {
            false
        }
    }

    private fun bindServiceSync(attempt: Int = 0): IDhizukuInstallerService? {
        if (attempt >= MAX_BIND_ATTEMPTS) {
            Log.e(TAG, "Exceeded max bind attempts ($MAX_BIND_ATTEMPTS)")
            return null
        }

        if (attempt > 0) {
            clearStaleDhizukuClientCache(userServiceArgs())
            Thread.sleep(BIND_RETRY_DELAY_MS * attempt)
            if (attempt >= 2) {
                stopUserServiceQuietly()
            }
        }

        val args = userServiceArgs()
        val latch = CountDownLatch(1)
        var boundService: IDhizukuInstallerService? = null
        var disconnectedDuringBind = false

        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val alive = isBinderAlive(binder)
                if (!alive) {
                    clearStaleDhizukuClientCache(args)
                    return
                }
                boundService = IDhizukuInstallerService.Stub.asInterface(binder)
                synchronized(bindLock) {
                    installerService = boundService
                    serviceConnection = this
                }
                latch.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                disconnectedDuringBind = true
                synchronized(bindLock) {
                    installerService = null
                    serviceConnection = null
                }
                latch.countDown()
            }
        }

        val bound = Dhizuku.bindUserService(args, connection)
        if (!bound) {
            try {
                Dhizuku.unbindUserService(connection)
            } catch (_: Exception) {
            }
            return bindServiceSync(attempt + 1)
        }

        val connected = latch.await(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!connected) {
            try {
                Dhizuku.unbindUserService(connection)
            } catch (_: Exception) {
            }
            clearStaleDhizukuClientCache(args)
            return bindServiceSync(attempt + 1)
        }

        val service = boundService
        val aliveAfterBind = service != null && isBinderAlive(service.asBinder())
        if (!aliveAfterBind || disconnectedDuringBind) {
            synchronized(bindLock) {
                installerService = null
                serviceConnection = null
            }
            try {
                Dhizuku.unbindUserService(connection)
            } catch (_: Exception) {
            }
            clearStaleDhizukuClientCache(args)
            return bindServiceSync(attempt + 1)
        }

        return service
    }

    private fun readApkIdentity(apkPath: String): Pair<String?, Long> {
        val info = context.packageManager.getPackageArchiveInfo(
            apkPath,
            PackageManager.GET_ACTIVITIES,
        ) ?: return null to -1L
        info.applicationInfo?.apply {
            sourceDir = apkPath
            publicSourceDir = apkPath
        }
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        return info.packageName to versionCode
    }

    private fun shouldRetryBind(error: Exception): Boolean {
        if (error is DeadObjectException) return true
        if (error.cause is DeadObjectException || error.cause is RemoteException) return true
        return isDhizukuStartupError(error)
    }

    private fun isDhizukuStartupError(error: Exception): Boolean {
        val message = buildString {
            append(error.message.orEmpty())
            error.cause?.message?.let { append(' ').append(it) }
        }
        return message.contains("disconnect", ignoreCase = true) ||
            message.contains("binder", ignoreCase = true) ||
            message.contains("connection", ignoreCase = true) ||
            message.contains("DeadObject", ignoreCase = true) ||
            message.contains("Failed to bind", ignoreCase = true) ||
            message.contains("remote call failed", ignoreCase = true) ||
            message.contains("does not belong", ignoreCase = true) ||
            message.contains("frozen", ignoreCase = true) ||
            message.contains("No definition found", ignoreCase = true) ||
            message.contains("Modules configuration", ignoreCase = true)
    }

    private fun formatRemoteError(error: RemoteException): String {
        return error.message?.takeIf { it.isNotBlank() }
            ?: error.cause?.message?.takeIf { it.isNotBlank() }
            ?: "Dhizuku installer remote call failed"
    }

    companion object {
        private val dhizukuOperationLock = Any()
        private const val TAG = "DhizukuInstallManager"
        private const val BIND_TIMEOUT_SECONDS = 15L
        private const val MAX_INSTALL_ATTEMPTS = 3
        private const val MAX_BIND_ATTEMPTS = 3
        private const val STOP_SETTLE_MS = 400L
        private const val BIND_RETRY_DELAY_MS = 500L
        private const val DHIZUKU_STARTUP_SETTLE_MS = 1500L
    }
}
