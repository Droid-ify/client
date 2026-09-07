package com.looker.droidify.installer.model

import com.looker.droidify.data.model.PackageName

class InstallItem(
    val packageName: PackageName,
    val installFileName: String,
    val unarchiveId: Int? = null,
)
