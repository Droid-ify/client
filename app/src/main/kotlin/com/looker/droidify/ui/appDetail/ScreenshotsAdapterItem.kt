package com.looker.droidify.ui.appDetail

import com.looker.droidify.model.Product
import com.looker.droidify.model.Repository
import com.looker.droidify.ui.appDetail.ScreenshotsAdapter.ViewType

sealed interface ScreenshotsAdapterItem {
    val viewType: ViewType
}

data class ScreenshotItem(
    @JvmField
    val repository: Repository,
    @JvmField
    val packageName: String,
    @JvmField
    val screenshot: Product.Screenshot,
) : ScreenshotsAdapterItem {
    override val viewType: ViewType get() = ViewType.SCREENSHOT
}

data class VideoItem(
    @JvmField
    val videoUrl: String
) : ScreenshotsAdapterItem {
    override val viewType: ViewType get() = ViewType.VIDEO
}
