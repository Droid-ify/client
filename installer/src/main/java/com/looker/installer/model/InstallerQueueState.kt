package com.looker.installer.model

data class InstallerQueueState(
	val currentItem: InstallItemState,
	val queued: Set<String>
) {
	companion object {
		val EMPTY = InstallerQueueState(InstallItemState.EMPTY, emptySet())
	}
}

infix fun String.isQueuedIn(state: InstallerQueueState): Boolean = this in state.queued

infix fun String.isInstalling(state: InstallerQueueState): Boolean =
	this == state.currentItem.installedItem.packageName.name
			&& state.currentItem.state == InstallState.Installing