package com.looker.droidify.installer.model

enum class InstallState { Failed, Pending, Installing, Installed }

inline val InstallState.isCancellable: Boolean
    get() = this == InstallState.Pending
