package com.looker.installer.model

import com.looker.core.domain.PackageName
import com.looker.core.domain.toPackageName

data class InstallItem(
    val packageName: com.looker.core.domain.PackageName,
    val installFileName: String
)

infix fun String.installFrom(fileName: String) = InstallItem(this.toPackageName(), fileName)
