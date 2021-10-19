package com.looker.droidify.utility

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.looker.droidify.R
import com.looker.droidify.content.Cache
import com.looker.droidify.content.Preferences
import com.looker.droidify.entity.InstalledItem
import com.looker.droidify.entity.Product
import com.looker.droidify.entity.Repository
import com.looker.droidify.service.Connection
import com.looker.droidify.service.DownloadService
import com.looker.droidify.utility.extension.android.Android
import com.looker.droidify.utility.extension.android.singleSignature
import com.looker.droidify.utility.extension.android.versionCodeCompat
import com.looker.droidify.utility.extension.resources.getColorFromAttr
import com.looker.droidify.utility.extension.resources.getDrawableCompat
import com.looker.droidify.utility.extension.text.hex
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException
import java.util.*

object Utils {
    private fun createDefaultApplicationIcon(context: Context, tintAttrResId: Int): Drawable {
        return context.getDrawableCompat(R.drawable.ic_application_default).mutate()
            .apply { setTintList(context.getColorFromAttr(tintAttrResId)) }
    }

    fun PackageInfo.toInstalledItem(): InstalledItem {
        val signatureString = singleSignature?.let(Utils::calculateHash).orEmpty()
        return InstalledItem(packageName, versionName.orEmpty(), versionCodeCompat, signatureString)
    }

    fun getDefaultApplicationIcons(context: Context): Pair<Drawable, Drawable> {
        val progressIcon: Drawable =
            createDefaultApplicationIcon(context, android.R.attr.textColorSecondary)
        val defaultIcon: Drawable =
            createDefaultApplicationIcon(context, android.R.attr.colorAccent)
        return Pair(progressIcon, defaultIcon)
    }

    fun getToolbarIcon(context: Context, resId: Int): Drawable {
        val drawable = context.getDrawableCompat(resId).mutate()
        if (Android.sdk(23)) {
            drawable.setTintList(context.getColorFromAttr(R.attr.colorOnPrimarySurface))
        }
        return drawable
    }

    fun calculateHash(signature: Signature): String {
        return MessageDigest.getInstance("MD5").digest(signature.toCharsString().toByteArray())
            .hex()
    }

    fun calculateFingerprint(certificate: Certificate): String {
        val encoded = try {
            certificate.encoded
        } catch (e: CertificateEncodingException) {
            null
        }
        return encoded?.let(::calculateFingerprint).orEmpty()
    }

    fun calculateFingerprint(key: ByteArray): String {
        return if (key.size >= 256) {
            try {
                val fingerprint = MessageDigest.getInstance("SHA-256").digest(key)
                val builder = StringBuilder()
                for (byte in fingerprint) {
                    builder.append("%02X".format(Locale.US, byte.toInt() and 0xff))
                }
                builder.toString()
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        } else {
            ""
        }
    }

    fun areAnimationsEnabled(context: Context): Boolean {
        return if (Android.sdk(26)) {
            ValueAnimator.areAnimatorsEnabled()
        } else {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) != 0f
        }
    }

    suspend fun Activity.startPackageInstaller(cacheFileName: String) {
        val file = Cache.getReleaseFile(this, cacheFileName)
        if (Preferences[Preferences.Key.RootPermission]) {
            val commandBuilder = StringBuilder()
            val verifyState = getVerifyState()
            if (verifyState == "1") commandBuilder.append("settings put global verifier_verify_adb_installs 0 ; ")
            commandBuilder.append(getPackageInstallCommand(file))
            commandBuilder.append(" ; settings put global verifier_verify_adb_installs $verifyState")
            withContext(Dispatchers.IO) {
                launch {
                    val result = Shell.su(commandBuilder.toString()).exec()
                    launch {
                        if (result.isSuccess) {
                            Shell.su("${getUtilBoxPath()} rm ${quote(file.absolutePath)}").submit()
                        }
                    }
                }
            }
        } else {
            val (uri, flags) = if (Android.sdk(24)) {
                Pair(
                    Cache.getReleaseUri(this, cacheFileName),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } else {
                Pair(Uri.fromFile(file), 0)
            }
            // TODO Handle deprecation
            @Suppress("DEPRECATION")
            startActivity(
                Intent(Intent.ACTION_INSTALL_PACKAGE)
                    .setDataAndType(uri, "application/vnd.android.package-archive").setFlags(flags)
            )
        }
    }

    fun startUpdate(
        packageName: String,
        installedItem: InstalledItem?,
        products: List<Pair<Product, Repository>>,
        downloadConnection: Connection<DownloadService.Binder, DownloadService>
    ) {
        val productRepository = Product.findSuggested(products, installedItem) { it.first }
        val compatibleReleases = productRepository?.first?.selectedReleases.orEmpty()
            .filter { installedItem == null || installedItem.signature == it.signature }
        val release = if (compatibleReleases.size >= 2) {
            compatibleReleases
                .filter { it.platforms.contains(Android.primaryPlatform) }
                .minByOrNull { it.platforms.size }
                ?: compatibleReleases.minByOrNull { it.platforms.size }
                ?: compatibleReleases.firstOrNull()
        } else {
            compatibleReleases.firstOrNull()
        }
        val binder = downloadConnection.binder
        if (productRepository != null && release != null && binder != null) {
            binder.enqueue(
                packageName,
                productRepository.first.name,
                productRepository.second,
                release
            )
        } else Unit
    }

    private fun getPackageInstallCommand(cacheFile: File): String =
        "cat \"${cacheFile.absolutePath}\" | pm install -t -r -S ${cacheFile.length()}"

    private fun getVerifyState(): String =
        Shell.sh("settings get global verifier_verify_adb_installs").exec().out[0]

    private fun quote(string: String) =
        "\"${string.replace(Regex("""[\\$"`]""")) { c -> "\\${c.value}" }}\""

    private fun getUtilBoxPath(): String {
        listOf("toybox", "busybox").forEach {
            var shellResult = Shell.su("which $it").exec()
            if (shellResult.out.isNotEmpty()) {
                val utilBoxPath = shellResult.out.joinToString("")
                if (utilBoxPath.isNotEmpty()) {
                    val utilBoxQuoted = quote(utilBoxPath)
                    shellResult = Shell.su("$utilBoxQuoted --version").exec()
                    if (shellResult.out.isNotEmpty()) {
                        val utilBoxVersion = shellResult.out.joinToString("")
                        Log.i(
                            this.javaClass.canonicalName,
                            "Using Utilbox $it : $utilBoxQuoted $utilBoxVersion"
                        )
                    }
                    return utilBoxQuoted
                }
            }
        }
        return ""
    }
}
