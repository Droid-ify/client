package com.looker.installer.model

import com.looker.core.domain.model.PackageName
import com.looker.core.domain.model.toPackageName

data class InstallItem(
    val packageName: PackageName,
    val installFileName: String
)

infix fun String.installFrom(fileName: String) = InstallItem(this.toPackageName(), fileName)
