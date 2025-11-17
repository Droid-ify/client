package com.looker.droidify.installer.installers

import com.looker.droidify.data.model.PackageName
import com.looker.droidify.installer.model.InstallItem
import com.looker.droidify.installer.model.InstallState

interface Installer : AutoCloseable {

    suspend fun install(installItem: InstallItem): InstallState

    suspend fun uninstall(packageName: PackageName)

}
