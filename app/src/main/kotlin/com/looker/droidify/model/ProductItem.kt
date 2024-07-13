package com.looker.droidify.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class ProductItem(
    var repositoryId: Long,
    var packageName: String,
    var name: String,
    var summary: String,
    val icon: String,
    val metadataIcon: String,
    val version: String,
    var installedVersion: String,
    var compatible: Boolean,
    var canUpdate: Boolean,
    var matchRank: Int
) {
    sealed class Section : Parcelable {

        @Parcelize
        data object All : Section()

        @Parcelize
        data class Category(val name: String) : Section()

        @Parcelize
        data class Repository(val id: Long, val name: String) : Section()
    }
}
