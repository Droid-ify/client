package com.looker.droidify.data.local.model

import com.looker.droidify.sync.v2.model.LocalizedIcon
import com.looker.droidify.sync.v2.model.LocalizedString

data class GraphicEntity(
    val tvBanner: LocalizedIcon?,
    val video: LocalizedString?,
    val promoGraphic: LocalizedIcon?,
    val featureGraphic: LocalizedIcon?,
    val id: Int = -1,
)
