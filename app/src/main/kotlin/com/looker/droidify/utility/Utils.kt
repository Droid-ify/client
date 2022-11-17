package com.looker.droidify.utility

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.graphics.drawable.Drawable
import com.looker.core.common.extension.getColorFromAttr
import com.looker.core.common.extension.getDrawableCompat
import com.looker.core.common.hex
import com.looker.core.model.InstalledItem
import com.looker.core.model.Product
import com.looker.core.model.Repository
import com.looker.droidify.R
import com.looker.droidify.service.Connection
import com.looker.droidify.service.DownloadService
import com.looker.droidify.utility.extension.android.Android
import com.looker.droidify.utility.extension.android.singleSignature
import com.looker.droidify.utility.extension.android.versionCodeCompat
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

	fun Certificate.fingerprint(): String {
		val encoded = try {
			encoded
		} catch (e: CertificateEncodingException) {
			null
		}
		return encoded?.fingerprint().orEmpty()
	}

	fun ByteArray.fingerprint(): String = if (size >= 256) {
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

	fun startUpdate(
		packageName: String,
		installedItem: InstalledItem?,
		products: List<Pair<Product, Repository>>,
		downloadConnection: Connection<DownloadService.Binder, DownloadService>,
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
		}
	}
}
