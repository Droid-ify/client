package com.looker.droidify.utility.extension

import com.looker.droidify.content.Preferences
import com.looker.installer.model.*

@InstallerType
val Preferences.InstallerType.toIntDef : Int
	get() = when(this) {
		Preferences.InstallerType.Legacy -> TYPE_LEGACY
		Preferences.InstallerType.Session -> TYPE_SESSION
		Preferences.InstallerType.Shizuku -> TYPE_SHIZUKU
		Preferences.InstallerType.Root -> TYPE_ROOT
	}