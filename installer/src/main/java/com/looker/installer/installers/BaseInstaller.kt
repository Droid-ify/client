package com.looker.installer.installers

import com.looker.core.model.newer.PackageName
import com.looker.installer.model.InstallItem
import com.looker.installer.model.InstallItemState
import kotlinx.coroutines.flow.MutableStateFlow

interface BaseInstaller {

	suspend fun performInstall(installItem: InstallItem, state: MutableStateFlow<InstallItemState>)

	suspend fun performUninstall(packageName: PackageName)

}