package com.looker.droidify.data.local.model

import com.looker.droidify.sync.v2.model.LocalizedFiles

data class ScreenshotEntity(
    val phone: LocalizedFiles?,
    val sevenInch: LocalizedFiles?,
    val tenInch: LocalizedFiles?,
    val wear: LocalizedFiles?,
    val tv: LocalizedFiles?,
    val id: Int = -1,
)
