package com.looker.installer.model

import com.looker.core.common.PackageName
import com.looker.core.common.toPackageName

data class InstallItem(
	val packageName: PackageName,
	val installFileName: String
)

infix fun String.installFrom(fileName: String) = InstallItem(this.toPackageName(), fileName)
