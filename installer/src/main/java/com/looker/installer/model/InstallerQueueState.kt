package com.looker.installer.model

data class InstallerQueueState(
	val currentItem: InstallItemState,
	val queued: Set<String>
) {
	companion object {
		val EMPTY = InstallerQueueState(InstallItemState.EMPTY, emptySet())
	}
}

operator fun InstallerQueueState.contains(packageName: String): Boolean {
	return packageName in queued || packageName isInstalling this
}

infix fun String.isInstalling(state: InstallerQueueState): Boolean =
	this == state.currentItem.currentItem.packageName.name
			&& state.currentItem.state == InstallState.Installing