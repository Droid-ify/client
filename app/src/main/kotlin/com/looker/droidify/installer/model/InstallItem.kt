package com.looker.droidify.installer.model

import com.looker.droidify.domain.model.PackageName
import com.looker.droidify.domain.model.toPackageName

class InstallItem(
    val packageName: PackageName,
    val installFileName: String,
    val unarchiveId: Int? = null
)

infix fun String.installFrom(fileName: String) = InstallItem(this.toPackageName(), fileName)
