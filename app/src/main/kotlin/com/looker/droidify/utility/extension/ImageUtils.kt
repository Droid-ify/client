package com.looker.droidify.utility.extension

import android.view.View
import com.looker.core.common.extension.dpi
import com.looker.core.common.nullIfEmpty
import com.looker.core.model.Product
import com.looker.core.model.ProductItem
import com.looker.core.model.Repository

private val SUPPORTED_DPI = listOf(120, 160, 240, 320, 480, 640)

fun Product.Screenshot.url(repository: Repository, packageName: String): String {
	val phoneType = when (type) {
		Product.Screenshot.Type.PHONE -> "phoneScreenshots"
		Product.Screenshot.Type.SMALL_TABLET -> "sevenInchScreenshots"
		Product.Screenshot.Type.LARGE_TABLET -> "tenInchScreenshots"
	}
	return "${repository.randomAddress}/$packageName/${locale}/$phoneType/${path}"
}

fun ProductItem.icon(
	view: View,
	repository: Repository
): String? {
	val address = repository.randomAddress.nullIfEmpty() ?: return null
	val packageNameCalculated = packageName.nullIfEmpty() ?: return null
	val iconCal = icon.nullIfEmpty()
	val metadataIconCalc = metadataIcon.nullIfEmpty()
	val dpi = (SUPPORTED_DPI.find { it >= view.dpi } ?: SUPPORTED_DPI.last()).toString()
		.nullIfEmpty()
	val path = when {
		iconCal != null -> "${if (dpi != null) "icons-$dpi" else "icons"}/$iconCal"
		metadataIconCalc != null -> "$packageNameCalculated/$metadataIconCalc"
		else -> return null
	}
	return "$address/$path"
}