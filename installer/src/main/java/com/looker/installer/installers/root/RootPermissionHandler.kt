package com.looker.installer.installers.root

import com.topjohnwu.superuser.Shell

class RootPermissionHandler {

    val isGranted: Boolean
        get() {
            Shell.getCachedShell() ?: Shell.getShell()
            return Shell.isAppGrantedRoot() ?: false
        }
}
