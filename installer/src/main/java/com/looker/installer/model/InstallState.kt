package com.looker.installer.model

sealed interface InstallState {

	object Failed : InstallState

	object Installing : InstallState

	object Installed : InstallState

}