package com.looker.droidify.utility.extension

import com.looker.core.model.InstalledItem
import com.looker.core.model.Product
import com.looker.core.model.Repository
import com.looker.droidify.service.Connection
import com.looker.droidify.service.DownloadService
import com.looker.droidify.utility.extension.android.Android

fun Connection<DownloadService.Binder, DownloadService>.startUpdate(
	packageName: String,
	installedItem: InstalledItem?,
	products: List<Pair<Product, Repository>>,
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
	val binder = binder
	if (productRepository != null && release != null && binder != null) {
		binder.enqueue(
			packageName = packageName,
			name = productRepository.first.name,
			repository = productRepository.second,
			release = release,
			isUpdate = installedItem != null
		)
	}
}