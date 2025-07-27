package com.looker.droidify.installer.model

import com.looker.droidify.data.model.PackageName
import com.looker.droidify.data.model.toPackageName

class InstallItem(
    val packageName: PackageName,
    val installFileName: String
)

infix fun String.installFrom(fileName: String) = InstallItem(this.toPackageName(), fileName)
