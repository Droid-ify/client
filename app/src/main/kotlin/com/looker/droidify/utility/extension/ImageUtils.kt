package com.looker.droidify.utility.extension

import android.view.View
import com.looker.core.common.Singleton
import com.looker.core.common.extension.dpi
import com.looker.droidify.model.Product
import com.looker.droidify.model.ProductItem
import com.looker.droidify.model.Repository

object ImageUtils {
    private val SUPPORTED_DPI = listOf(120, 160, 240, 320, 480, 640)
    private var DeviceDpi = Singleton<String>()

    fun Product.Screenshot.url(
        repository: Repository,
        packageName: String
    ): String {
        val phoneType = when (type) {
            Product.Screenshot.Type.PHONE -> "phoneScreenshots"
            Product.Screenshot.Type.SMALL_TABLET -> "sevenInchScreenshots"
            Product.Screenshot.Type.LARGE_TABLET -> "tenInchScreenshots"
        }
        return "${repository.address}/$packageName/$locale/$phoneType/$path"
    }

    fun ProductItem.icon(
        view: View,
        repository: Repository
    ): String? {
        if (packageName.isBlank()) return null
        if (icon.isBlank() && metadataIcon.isBlank()) return null
        if (repository.version < 11 && icon.isNotBlank()) {
            return "${repository.address}/icons/$icon"
        }
        if (icon.isNotBlank()) {
            val deviceDpi = DeviceDpi.getOrUpdate {
                (SUPPORTED_DPI.find { it >= view.dpi } ?: SUPPORTED_DPI.last()).toString()
            }
            return "${repository.address}/icons-$deviceDpi/$icon"
        }
        if (metadataIcon.isNotBlank()) {
            return "${repository.address}/$packageName/$metadataIcon"
        }
        return null
    }
}
