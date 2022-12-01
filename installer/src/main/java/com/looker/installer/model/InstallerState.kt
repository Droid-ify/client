package com.looker.installer.model

sealed interface InstallerState {

	object Error : InstallerState

	object Queued : InstallerState

	object Installing : InstallerState

	object Installed : InstallerState

}