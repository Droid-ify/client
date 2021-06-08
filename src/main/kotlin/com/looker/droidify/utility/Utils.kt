package com.looker.droidify.utility

import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.Signature
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.LocaleList
import android.provider.Settings
import com.looker.droidify.BuildConfig
import com.looker.droidify.R
import com.looker.droidify.utility.extension.android.Android
import com.looker.droidify.utility.extension.resources.getColorFromAttr
import com.looker.droidify.utility.extension.resources.getDrawableCompat
import com.looker.droidify.utility.extension.text.hex
import java.security.MessageDigest
import java.security.cert.Certificate
import java.security.cert.CertificateEncodingException
import java.util.*

object Utils {
    private fun createDefaultApplicationIcon(context: Context, tintAttrResId: Int): Drawable {
        return context.getDrawableCompat(R.drawable.ic_application_default).mutate()
            .apply { setTintList(context.getColorFromAttr(tintAttrResId)) }
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
        drawable.setTintList(context.getColorFromAttr(android.R.attr.titleTextColor))
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

    fun configureLocale(context: Context): Context {
        val supportedLanguages = BuildConfig.LANGUAGES.toSet()
        val configuration = context.resources.configuration
        val currentLocales = if (Android.sdk(24)) {
            val localesList = configuration.locales
            (0 until localesList.size()).map(localesList::get)
        } else {
            @Suppress("DEPRECATION")
            listOf(configuration.locale)
        }
        val compatibleLocales = currentLocales
            .filter { it.language in supportedLanguages }
            .let { if (it.isEmpty()) listOf(Locale.US) else it }
        Locale.setDefault(compatibleLocales.first())
        val newConfiguration = Configuration(configuration)
        if (Android.sdk(24)) {
            newConfiguration.setLocales(LocaleList(*compatibleLocales.toTypedArray()))
        } else {
            @Suppress("DEPRECATION")
            newConfiguration.locale = compatibleLocales.first()
        }
        return context.createConfigurationContext(newConfiguration)
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
}
