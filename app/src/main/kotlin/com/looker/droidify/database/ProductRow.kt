package com.looker.droidify.database

import androidx.annotation.IntDef
import com.looker.droidify.model.ProductItem
import com.looker.droidify.model.Repository
import com.looker.droidify.utility.common.nullIfEmpty

sealed interface AppListRow {
    @AppListRowViewType
    val viewType: Int
}

data class ProductRow(
    @JvmField
    val productItem: ProductItem,
    @JvmField
    val repository: Repository?,
    @JvmField
    val versionText: String?
): AppListRow {

    @JvmField
    val enabled: Boolean = productItem.compatible || productItem.installedVersion.isNotEmpty()

    @JvmField
    val isInstalled: Boolean = productItem.installedVersion.nullIfEmpty() != null

    @JvmField
    val summaryVisible: Boolean = productItem.summary.isNotEmpty()
        && productItem.name != productItem.summary

    @JvmField
    val summary: String? = kotlin.run {
        if (summaryVisible) {
            productItem.summary
        } else {
            null
        }
    }

    @AppListRowViewType
    override val viewType: Int
        get() = AppListRowViewType.PRODUCT
}

fun createProductRow(
    updates: Boolean,
    productItem: ProductItem,
    repository: Repository?,
): ProductRow {

    return ProductRow(
        productItem = productItem,
        repository = repository,
        versionText = if (updates) {
            productItem.version
        } else {
            productItem.installedVersion.nullIfEmpty() ?: productItem.version
        }
    )
}

data class EmptyListRow(
    @JvmField
    val emptyText: String,
): AppListRow {

    @AppListRowViewType
    override val viewType: Int
        get() = AppListRowViewType.EMPTY
}

@IntDef(
    value = [
        AppListRowViewType.PRODUCT,
        AppListRowViewType.EMPTY,
    ]
)
annotation class AppListRowViewType {
    companion object {
        const val PRODUCT = 0
        const val EMPTY = 1
    }
}
