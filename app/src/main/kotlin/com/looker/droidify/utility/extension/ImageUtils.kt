package com.looker.droidify.utility.extension

import android.view.View
import com.looker.core_model.Product
import com.looker.core_model.Repository
import com.looker.droidify.network.CoilDownloader

fun Product.Screenshot.url(
	repository: Repository,
	packageName: String
): String = CoilDownloader.createScreenshotUri(
	repository = repository,
	packageName = packageName,
	screenshot = this
).toString()

fun String.icon(
	view: View,
	icon: String,
	metadataIcon: String,
	repository: Repository
): String = CoilDownloader.createIconUri(
	view = view,
	packageName = this,
	metadataIcon = metadataIcon,
	icon = icon,
	repository = repository
).toString()