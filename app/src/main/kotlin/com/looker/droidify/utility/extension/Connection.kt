package com.looker.droidify.utility.extension

import android.util.Log
import com.looker.droidify.model.InstalledItem
import com.looker.droidify.model.Product
import com.looker.droidify.model.Release
import com.looker.droidify.model.Repository
import com.looker.droidify.model.findSuggested
import com.looker.droidify.service.Connection
import com.looker.droidify.service.DownloadService
import com.looker.droidify.utility.common.log
import com.looker.droidify.utility.extension.android.Android

private const val TAG = "DroidifyUpdateAll"

fun Connection<DownloadService.Binder, DownloadService>.startUpdate(
    packageName: String,
    installedItem: InstalledItem?,
    products: List<Pair<Product, Repository>>,
) {
    val params = resolveUpdateParams(packageName, installedItem, products) ?: return
    requireNotNull(binder).enqueue(
        packageName = packageName,
        name = params.product.name,
        repository = params.repository,
        release = params.release,
        isUpdate = installedItem != null,
    )
}

suspend fun Connection<DownloadService.Binder, DownloadService>.startUpdateAndAwait(
    packageName: String,
    installedItem: InstalledItem?,
    products: List<Pair<Product, Repository>>,
): Boolean {
    val params = resolveUpdateParams(packageName, installedItem, products) ?: return false
    return requireNotNull(binder).enqueueAndAwait(
        packageName = packageName,
        name = params.product.name,
        repository = params.repository,
        release = params.release,
        isUpdate = installedItem != null,
    )
}

private data class UpdateParams(
    val product: Product,
    val repository: Repository,
    val release: Release,
)

private fun Connection<DownloadService.Binder, DownloadService>.resolveUpdateParams(
    packageName: String,
    installedItem: InstalledItem?,
    products: List<Pair<Product, Repository>>,
): UpdateParams? {
    if (binder == null) {
        log("startUpdate skipped $packageName: binder null", TAG, Log.ERROR)
        return null
    }
    if (products.isEmpty()) {
        log("startUpdate skipped $packageName: no products", TAG, Log.WARN)
        return null
    }

    val (product, repository) = products.findSuggested(installedItem) ?: run {
        log("startUpdate skipped $packageName: findSuggested returned null", TAG, Log.WARN)
        return null
    }

    val compatibleReleases = product.selectedReleases
        .filter { installedItem == null || installedItem.signature == it.signature }
    if (compatibleReleases.isEmpty()) {
        log("startUpdate skipped $packageName: no compatible release", TAG, Log.WARN)
        return null
    }

    val selectedRelease = compatibleReleases.singleOrNull() ?: compatibleReleases.run {
        filter { Android.primaryPlatform in it.platforms }.minByOrNull { it.platforms.size }
            ?: minByOrNull { it.platforms.size }
            ?: firstOrNull()
    }
    if (selectedRelease == null) {
        log("startUpdate skipped $packageName: no selected release", TAG, Log.WARN)
        return null
    }

    return UpdateParams(product, repository, selectedRelease)
}
