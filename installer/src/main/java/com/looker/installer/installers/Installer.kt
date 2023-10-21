package com.looker.installer.installers

import com.looker.core.common.PackageName
import com.looker.installer.model.InstallItem
import com.looker.installer.model.InstallState

interface Installer {

    suspend fun install(installItem: InstallItem): InstallState

    suspend fun uninstall(packageName: PackageName)

    fun cleanup()
}
