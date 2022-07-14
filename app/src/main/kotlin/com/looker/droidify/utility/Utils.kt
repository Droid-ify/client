package com.looker.droidify.utility

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import com.looker.core_common.hex
import com.looker.core_model.InstalledItem
import com.looker.core_model.Product
import com.looker.core_model.Repository
import com.looker.droidify.BuildConfig
import com.looker.droidify.Common.PREFS_LANGUAGE_DEFAULT
import com.looker.droidify.R
import com.looker.droidify.content.Preferences
import com.looker.droidify.service.Connection
import com.looker.droidify.service.DownloadService
import com.looker.droidify.utility.extension.android.Android
import com.looker.droidify.utility.extension.android.singleSignature
import com.looker.droidify.utility.extension.android.versionCodeCompat
import com.looker.droidify.utility.extension.resources.getColorFromAttr
import com.looker.droidify.utility.extension.resources.getDrawableCompat
import kotlinx.coroutines.flow.MutableStateFlow
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
		val signatureString = singleSignature?.calculateHash.orEmpty()
		return InstalledItem(
			packageName,
			versionName.orEmpty(),
			versionCodeCompat,
			signatureString
		)
	}

	fun getDefaultApplicationIcons(context: Context): Pair<Drawable, Drawable> {
		val progressIcon: Drawable =
			createDefaultApplicationIcon(context, android.R.attr.textColorSecondary)
		val defaultIcon: Drawable =
			createDefaultApplicationIcon(context, R.attr.colorAccent)
		return Pair(progressIcon, defaultIcon)
	}

	fun getToolbarIcon(context: Context, resId: Int): Drawable {
		return context.getDrawableCompat(resId).mutate()
	}

	inline val Signature.calculateHash
		get() = MessageDigest.getInstance("MD5")
			.digest(toCharsString().toByteArray())
			.hex()

	inline val Certificate.fingerprint: String
		get() {
			val encoded = try {
				encoded
			} catch (e: CertificateEncodingException) {
				null
			}
			return encoded?.fingerprint.orEmpty()
		}

	inline val ByteArray.fingerprint: String
		get() {
			return if (size >= 256) {
				try {
					val fingerprint = MessageDigest.getInstance("SHA-256").digest(this)
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

	suspend fun startUpdate(
		packageName: String,
		installedItem: InstalledItem?,
		products: List<Pair<Product, Repository>>,
		downloadConnection: Connection<DownloadService.Binder, DownloadService>,
	) {
		val productRepository = Product.findSuggested(products, installedItem) { it.first }
		val compatibleReleases = productRepository?.first?.selectedReleases.orEmpty()
			.filter { installedItem == null || installedItem.signature == it.signature }
		val releaseFlow = MutableStateFlow(compatibleReleases.firstOrNull())
		if (compatibleReleases.size > 1) {
			releaseFlow.emit(
				compatibleReleases
					.filter { it.platforms.contains(Android.primaryPlatform) }
					.minByOrNull { it.platforms.size }
					?: compatibleReleases.minByOrNull { it.platforms.size }
					?: compatibleReleases.firstOrNull()
			)
		}
		val binder = downloadConnection.binder
		releaseFlow.collect {
			if (productRepository != null && it != null && binder != null) {
				binder.enqueue(
					packageName,
					productRepository.first.name,
					productRepository.second,
					it
				)
			}
		}
	}

	fun Context.setLanguage(): Configuration {
		var setLocalCode = Preferences[Preferences.Key.Language]
		if (setLocalCode == PREFS_LANGUAGE_DEFAULT) {
			setLocalCode = Locale.getDefault().toString()
		}
		val config = resources.configuration
		val sysLocale = if (Android.sdk(24)) config.locales[0] else config.locale
		if (setLocalCode != sysLocale.toString() || setLocalCode != "${sysLocale.language}-r${sysLocale.country}") {
			val newLocale = getLocaleOfCode(setLocalCode)
			Locale.setDefault(newLocale)
			config.setLocale(newLocale)
		}
		return config
	}

	val languagesList: List<String>
		get() {
			val entryVals = arrayOfNulls<String>(1)
			entryVals[0] = PREFS_LANGUAGE_DEFAULT
			return entryVals.plus(BuildConfig.DETECTED_LOCALES.sorted()).filterNotNull()
		}

	fun translateLocale(locale: Locale): String {
		val country = locale.getDisplayCountry(locale)
		val language = locale.getDisplayLanguage(locale)
		return (language.replaceFirstChar { it.uppercase(Locale.getDefault()) }
				+ (if (country.isNotEmpty() && country.compareTo(language, true) != 0)
			"($country)" else ""))
	}

	fun Context.getLocaleOfCode(localeCode: String): Locale = when {
		localeCode.isEmpty() -> if (Android.sdk(24)) {
			resources.configuration.locales[0]
		} else {
			resources.configuration.locale
		}
		localeCode.contains("-r") -> Locale(
			localeCode.substring(0, 2),
			localeCode.substring(4)
		)
		localeCode.contains("_") -> Locale(
			localeCode.substring(0, 2),
			localeCode.substring(3)
		)
		else -> Locale(localeCode)
	}
}
