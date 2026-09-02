package com.looker.droidify.installer.installers.dhizuku

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.util.Log
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.installer.installers.Installer
import com.looker.droidify.installer.installers.session.SessionInstallerReceiver
import com.looker.droidify.installer.installers.uninstallPackage
import com.looker.droidify.installer.model.InstallItem
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.utility.common.SdkCheck
import com.looker.droidify.utility.common.cache.Cache
import com.looker.droidify.utility.common.sdkAbove
import com.rosan.dhizuku.api.Dhizuku
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@SuppressLint("PrivateApi", "DiscouragedPrivateApi", "BlockedPrivateApi", "SoonBlockedPrivateApi", "RequestInstallPackages")
@Suppress("PrivateApi", "DiscouragedPrivateApi", "BlockedPrivateApi", "SoonBlockedPrivateApi")
class DhizukuInstaller(private val context: Context) : Installer {

    // public fallback installer (used only if privileged creation fails)
    private val fallbackInstaller = context.packageManager.packageInstaller
    private val intent = Intent(context, SessionInstallerReceiver::class.java)

    companion object {
        private const val TAG = "DhizukuInstaller"
        private val flags = if (SdkCheck.isSnowCake) PendingIntent.FLAG_MUTABLE else 0
    }

    @SuppressLint("RequestInstallPackages")
    override suspend fun install(installItem: InstallItem): InstallState =
        suspendCancellableCoroutine { cont ->
            val cacheFile = Cache.getReleaseFile(context, installItem.installFileName)
            Log.d(TAG, "install start ${installItem.packageName.name} file=${cacheFile.absolutePath} size=${cacheFile.length()} isFile=${cacheFile.isFile}")

            if (cacheFile.length() == 0L || !cacheFile.isFile) {
                Log.e(TAG, "File is not valid or empty: ${cacheFile.absolutePath}")
                if (cont.isActive) cont.resume(InstallState.Failed)
                return@suspendCancellableCoroutine
            }

            // Ensure Dhizuku is available and permission granted
            val granted = try {
                val initOk = Dhizuku.init(context)
                val grantedNow = Dhizuku.isPermissionGranted()
                Log.d(TAG, "Dhizuku init=$initOk granted=$grantedNow owner=${runCatching { Dhizuku.getOwnerPackageName() }.getOrNull()}")
                initOk && grantedNow
            } catch (e: Exception) {
                Log.e(TAG, "Dhizuku init/check failed", e)
                false
            }
            if (!granted) {
                Log.e(TAG, "Dhizuku not granted / not available")
                if (cont.isActive) cont.resume(InstallState.Failed)
                return@suspendCancellableCoroutine
            }

            // Try to obtain Dhizuku-privileged PackageInstaller, fallback to public one
            val pkgInstaller: PackageInstaller
            val usingPrivileged: Boolean
            val privilegedResult = try {
                Pair(getDhizukuPackageInstaller(), true)
            } catch (e: Exception) {
                Log.w(TAG, "getDhizukuPackageInstaller failed, fallback to public installer", e)
                Pair(fallbackInstaller, false)
            }
            pkgInstaller = privilegedResult.first
            usingPrivileged = privilegedResult.second
            if (usingPrivileged) {
                Log.d(TAG, "Using Dhizuku-privileged PackageInstaller owner=${runCatching { Dhizuku.getOwnerPackageName() }.getOrNull()}")
            }

            val sessionId: Int
            try {
                sessionId = pkgInstaller.createSession(sessionParams(installItem))
                Log.d(TAG, "createSession ok id=$sessionId privileged=$usingPrivileged")
            } catch (e: Exception) {
                Log.e(TAG, "createSession failed", e)
                if (cont.isActive) cont.resume(InstallState.Failed)
                return@suspendCancellableCoroutine
            }

            val callback = object : PackageInstaller.SessionCallback() {
                override fun onCreated(sessionId: Int) {}
                override fun onBadgingChanged(sessionId: Int) {}
                override fun onActiveChanged(sessionId: Int, active: Boolean) {}
                override fun onProgressChanged(sessionId: Int, progress: Float) {}
                override fun onFinished(finishedId: Int, success: Boolean) {
                    Log.d(TAG, "SessionCallback.onFinished finishedId=$finishedId sessionId=$sessionId success=$success contActive=${cont.isActive}")
                    if (finishedId != sessionId || !cont.isActive) return
                    try { pkgInstaller.unregisterSessionCallback(this) } catch (_: Exception) {}
                    cont.resume(if (success) InstallState.Installed else InstallState.Failed)
                }
            }

            cont.invokeOnCancellation {
                Log.d(TAG, "install cancelled for $sessionId")
                try { pkgInstaller.unregisterSessionCallback(callback) } catch (_: Exception) {}
                try { pkgInstaller.abandonSession(sessionId) } catch (e: SecurityException) {
                    Log.w(TAG, "abandonSession failed", e)
                } catch (_: Exception) {}
            }

            pkgInstaller.registerSessionCallback(callback, Handler(Looper.getMainLooper()))

            var session: PackageInstaller.Session? = null
            try {
                session = pkgInstaller.openSession(sessionId)
                Log.d(TAG, "openSession ok id=$sessionId")

                // Always try to wrap mSession via Dhizuku, even for privileged installer (mirrors InstallerX setSessionIBinder)
                wrapSessionBinder(session)

                session.use { activeSession ->
                    val sizeBytes = cacheFile.length()
                    Log.d(TAG, "openWrite ${cacheFile.name} size=$sizeBytes")
                    cacheFile.inputStream().use { fileStream ->
                        activeSession.openWrite(cacheFile.name, 0, sizeBytes).use { output ->
                            if (cont.isActive) {
                                val copied = fileStream.copyTo(output)
                                Log.d(TAG, "copyTo done copied=$copied")
                                activeSession.fsync(output)
                                Log.d(TAG, "fsync done")
                            } else {
                                Log.w(TAG, "cont not active before copy, aborting")
                            }
                        }
                    }
                    val pendingIntent = PendingIntent.getBroadcast(context, sessionId, intent, flags)
                    Log.d(TAG, "commit sessionId=$sessionId intentSender=$pendingIntent contActive=${cont.isActive}")
                    if (cont.isActive) {
                        activeSession.commit(pendingIntent.intentSender)
                        Log.d(TAG, "commit called")
                    } else {
                        Log.w(TAG, "cont not active before commit, skipping")
                    }
                }
                // Do NOT close pkgInstaller; keep callback registered until onFinished fires
                // The coroutine now suspends waiting for callback. If callback never fires, it will hang.
                // To avoid hang, we also rely on SessionInstallerReceiver broadcast as secondary signal?
                // For now, rely on SessionCallback; the broadcast receiver will handle notifications in parallel.
            } catch (e: Exception) {
                Log.e(TAG, "install failed for ${installItem.packageName.name} sessionId=$sessionId", e)
                try { pkgInstaller.unregisterSessionCallback(callback) } catch (_: Exception) {}
                try { pkgInstaller.abandonSession(sessionId) } catch (_: Exception) {}
                if (cont.isActive) cont.resume(InstallState.Failed)
                try { session?.close() } catch (_: Exception) {}
            }
        }

    @SuppressLint("MissingPermission")
    override suspend fun uninstall(packageName: PackageName) =
        context.uninstallPackage(packageName)

    private fun ensureHiddenApiExempted() {
        try {
            val bypassClass = Class.forName("org.lsposed.hiddenapibypass.HiddenApiBypass")
            bypassClass.getMethod("addHiddenApiExemptions", String::class.java)
                .invoke(null, "L")
        } catch (_: Exception) {
        }
    }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    @Suppress("PrivateApi", "DiscouragedPrivateApi")
    private fun getDhizukuPackageInstaller(): PackageInstaller {
        ensureHiddenApiExempted()

        // ServiceManager.getService("package") -> IBinder
        val smClass = Class.forName("android.os.ServiceManager")
        val getService = smClass.getMethod("getService", String::class.java)
        val pmBinder = getService.invoke(null, "package") as IBinder
        Log.d(TAG, "ServiceManager package binder=$pmBinder")

        val wrappedPmBinder = Dhizuku.binderWrapper(pmBinder)
        Log.d(TAG, "wrappedPmBinder=$wrappedPmBinder")

        // IPackageManager.Stub.asInterface(IBinder)
        val pmStub = Class.forName("android.content.pm.IPackageManager${'$'}Stub")
        val pmAsInterface = pmStub.getMethod("asInterface", IBinder::class.java)
        val iPackageManager = pmAsInterface.invoke(null, wrappedPmBinder)
        Log.d(TAG, "iPackageManager=$iPackageManager")

        // iPackageManager.getPackageInstaller() -> IPackageInstaller
        val getPackageInstaller = iPackageManager.javaClass.getMethod("getPackageInstaller")
        val iPackageInstallerObj = getPackageInstaller.invoke(iPackageManager)
        Log.d(TAG, "iPackageInstallerObj=$iPackageInstallerObj")

        // IInterface -> IBinder
        val asBinder = iPackageInstallerObj.javaClass.getMethod("asBinder")
        val piBinder = asBinder.invoke(iPackageInstallerObj) as IBinder
        val wrappedPiBinder = Dhizuku.binderWrapper(piBinder)
        Log.d(TAG, "wrappedPiBinder=$wrappedPiBinder")

        val piStub = Class.forName("android.content.pm.IPackageInstaller${'$'}Stub")
        val piAsInterface = piStub.getMethod("asInterface", IBinder::class.java)
        val iPackageInstaller = piAsInterface.invoke(null, wrappedPiBinder)
        Log.d(TAG, "iPackageInstaller wrapped=$iPackageInstaller")

        val ownerPackage = Dhizuku.getOwnerPackageName()
        val userId = Process.myUid() / 100000
        Log.d(TAG, "ownerPackage=$ownerPackage userId=$userId")

        val piClass = Class.forName("android.content.pm.PackageInstaller")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // PackageInstaller(IPackageInstaller, String, String?, int)
            val ctor = piClass.getDeclaredConstructor(
                Class.forName("android.content.pm.IPackageInstaller"),
                String::class.java,
                String::class.java,
                Int::class.javaPrimitiveType
            )
            ctor.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            ctor.newInstance(iPackageInstaller, ownerPackage, null, userId) as PackageInstaller
        } else {
            val ctor = piClass.getDeclaredConstructor(
                Class.forName("android.content.pm.IPackageInstaller"),
                String::class.java,
                Int::class.javaPrimitiveType
            )
            ctor.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            ctor.newInstance(iPackageInstaller, ownerPackage, userId) as PackageInstaller
        }
    }

    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    @Suppress("PrivateApi", "DiscouragedPrivateApi")
    private fun wrapSessionBinder(session: PackageInstaller.Session) {
        try {
            ensureHiddenApiExempted()
            val field = session.javaClass.getDeclaredField("mSession")
            field.isAccessible = true
            val iInterface = field.get(session) ?: run {
                Log.w(TAG, "mSession is null, skipping wrap")
                return
            }
            val asBinder = iInterface.javaClass.getMethod("asBinder")
            val binder = asBinder.invoke(iInterface) as IBinder
            val wrappedBinder = Dhizuku.binderWrapper(binder)
            val stubClass = Class.forName("android.content.pm.IPackageInstallerSession${'$'}Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            val wrappedSession = asInterface.invoke(null, wrappedBinder)
            field.set(session, wrappedSession)
            Log.d(TAG, "mSession wrapped via Dhizuku.binderWrapper successfully")
        } catch (e: Exception) {
            Log.w(TAG, "wrapSessionBinder failed, proceeding without wrap (may still install but not silent)", e)
        }
    }

    @SuppressLint("RequestInstallPackages")
    private fun sessionParams(installItem: InstallItem) =
        PackageInstaller.SessionParams(MODE_FULL_INSTALL).apply {
            setAppPackageName(installItem.packageName.name)
            sdkAbove(sdk = Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            sdkAbove(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setRequestUpdateOwnership(true)
            }
            sdkAbove(sdk = Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                val unarchiveId = installItem.unarchiveId
                if (unarchiveId != null) {
                    setUnarchiveId(unarchiveId)
                }
            }
        }
}
