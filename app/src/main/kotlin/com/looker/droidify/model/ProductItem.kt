package com.looker.droidify.model

import android.os.Parcelable
import android.view.View
import com.looker.droidify.utility.common.extension.dpi
import kotlinx.parcelize.Parcelize

data class ProductItem(
    @JvmField
    val repoId: Long,
    @JvmField
    val packageName: String,
    @JvmField
    val name: String,
    @JvmField
    val summary: String,
    @JvmField
    val icon: String,
    @JvmField
    val metadataIcon: String,
    @JvmField
    val version: String,
    @JvmField
    val installedVersion: String,
    @JvmField
    val compatible: Boolean,
    @JvmField
    val canUpdate: Boolean,
    @JvmField
    val matchRank: Int,
) {
    sealed interface Section : Parcelable {

        @Parcelize
        object All : Section

        @Parcelize
        class Category(val name: String) : Section

        @Parcelize
        class Repository(val id: Long, val name: String) : Section
    }

    private var deviceDpi: Int = -1

    fun icon(
        view: View,
        repository: Repository
    ): String? {
        if (packageName.isBlank()) return null
        if (icon.isBlank() && metadataIcon.isBlank()) return null
        if (repository.version < 11 && icon.isNotBlank()) {
            return "${repository.address}/icons/$icon"
        }
        if (icon.isNotBlank()) {
            if (deviceDpi == -1) {
                deviceDpi = supportedDpi.find { it >= view.dpi } ?: supportedDpi.last()
            }
            return "${repository.address}/icons-$deviceDpi/$icon"
        }
        if (metadataIcon.isNotBlank()) {
            return "${repository.address}/$packageName/$metadataIcon"
        }
        return null
    }

    companion object {
        private val supportedDpi = intArrayOf(120, 160, 240, 320, 480, 640)
    }
}
