package com.looker.installer.model

sealed interface InstallEvent {

	data class Update(val installItem: InstallItem) : InstallEvent

	data class Install(val installItem: InstallItem) : InstallEvent

	data class Uninstall(val installItem: InstallItem) : InstallEvent

}