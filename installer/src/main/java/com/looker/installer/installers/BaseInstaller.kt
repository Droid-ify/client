package com.looker.installer.installers

import com.looker.core.model.newer.PackageName
import com.looker.installer.model.InstallItem
import com.looker.installer.model.InstallState

interface BaseInstaller {

	suspend fun performInstall(installItem: InstallItem): InstallState

	suspend fun performUninstall(packageName: PackageName)

	fun cleanup()

}