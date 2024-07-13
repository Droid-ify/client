package com.looker.installer.installers

import com.looker.core.domain.model.PackageName
import com.looker.installer.model.InstallItem
import com.looker.installer.model.InstallState

interface Installer : AutoCloseable {

    suspend fun install(installItem: InstallItem): InstallState

    suspend fun uninstall(packageName: PackageName)

}
