package com.looker.installer.model

import com.looker.core.model.newer.PackageName
import com.looker.core.model.newer.toPackageName

data class InstallItem(
	val packageName: PackageName,
	val installFileName: String
)

data class InstallItemState(
	val currentItem: InstallItem,
	val state: InstallState
) {
	companion object {
		val EMPTY = InstallItemState(InstallItem("".toPackageName(), ""), InstallState.Installed)
	}

	operator fun contains(name: String): Boolean =
		currentItem.packageName.name == name && state == InstallState.Installing
}

infix fun InstallItem.statesTo(state: InstallState) = InstallItemState(this, state)

infix fun String.installFrom(fileName: String) = InstallItem(this.toPackageName(), fileName)
