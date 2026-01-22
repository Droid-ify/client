package com.looker.droidify.ui.appDetail

import com.google.android.material.R

enum class SectionType(
    val titleResId: Int,
    val colorAttrResId: Int = R.attr.colorPrimary,
) {
    ANTI_FEATURES(com.looker.droidify.R.string.anti_features, R.attr.colorError),
    CHANGES(com.looker.droidify.R.string.changes),
    LINKS(com.looker.droidify.R.string.links),
    DONATE(com.looker.droidify.R.string.donate),
    PERMISSIONS(com.looker.droidify.R.string.permissions),
    VERSIONS(com.looker.droidify.R.string.versions)
}
