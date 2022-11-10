package com.looker.droidify.utility.extension

import android.view.View
import com.looker.core.common.nullIfEmpty
import com.looker.core.model.Product
import com.looker.core.model.Repository
import kotlin.math.roundToInt

private val supportedDpis = listOf(120, 160, 240, 320, 480, 640)

fun Product.Screenshot.url(repository: Repository, packageName: String): String {
	val phoneType = when (type) {
		Product.Screenshot.Type.PHONE -> "phoneScreenshots"
		Product.Screenshot.Type.SMALL_TABLET -> "sevenInchScreenshots"
		Product.Screenshot.Type.LARGE_TABLET -> "tenInchScreenshots"
	}
	return "${repository.address}/$packageName/${locale}/$phoneType/${path}"
}

fun String.icon(
	view: View,
	icon: String,
	metadataIcon: String,
	repository: Repository
): String {
	val address = repository.address.nullIfEmpty()
	val path = run {
		val packageNameCal =
			this.nullIfEmpty()
		val iconCal = icon.nullIfEmpty()
		val metadataIconCalc =
			metadataIcon.nullIfEmpty()
		val size = (view.layoutParams.let { kotlin.math.min(it.width, it.height) } /
				view.resources.displayMetrics.density).roundToInt()
		val displayDpi = view.context.resources.displayMetrics.densityDpi
		val requiredDpi = displayDpi * size / 48
		val dpi = (supportedDpis.find { it >= requiredDpi } ?: supportedDpis.last()).toString()
			.nullIfEmpty()
		when {
			iconCal != null -> "${if (dpi != null) "icons-$dpi" else "icons"}/$iconCal"
			packageNameCal != null && metadataIconCalc != null -> "$packageNameCal/$metadataIconCalc"
			else -> null
		}
	}
	return "$address/$path"
}