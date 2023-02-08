package com.looker.installer.model

sealed interface InstallState {

	object Failed : InstallState

	object Queued : InstallState

	object Installing : InstallState

	object Installed : InstallState

}