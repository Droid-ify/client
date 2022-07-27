package com.looker.droidify.utility.extension

import com.looker.core_datastore.model.InstallerType
import com.looker.droidify.content.Preferences

// FAKE
val Preferences.InstallerType.toIntDef: InstallerType
	get() = when (this) {
		Preferences.InstallerType.Legacy -> InstallerType.LEGACY
		Preferences.InstallerType.Session -> InstallerType.SESSION
		Preferences.InstallerType.Shizuku -> InstallerType.SHIZUKU
		Preferences.InstallerType.Root -> InstallerType.ROOT
	}