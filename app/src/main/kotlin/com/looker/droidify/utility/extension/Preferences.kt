package com.looker.droidify.utility.extension

import android.content.Context
import android.content.res.Configuration
import com.looker.core_datastore.model.AutoSync
import com.looker.core_datastore.model.InstallerType
import com.looker.core_datastore.model.ProxyType
import com.looker.core_datastore.model.Theme
import com.looker.droidify.R
import com.looker.droidify.content.Preferences
import com.looker.core_common.R.string as stringRes

// FAKE
val Preferences.InstallerType.toIntDef: InstallerType
	get() = when (this) {
		Preferences.InstallerType.Legacy -> InstallerType.LEGACY
		Preferences.InstallerType.Session -> InstallerType.SESSION
		Preferences.InstallerType.Shizuku -> InstallerType.SHIZUKU
		Preferences.InstallerType.Root -> InstallerType.ROOT
	}

fun Context?.themeName(theme: Theme) = this?.let {
	when (theme) {
		Theme.SYSTEM -> getString(stringRes.system)
		Theme.SYSTEM_BLACK -> getString(stringRes.system) + " " + getString(stringRes.amoled)
		Theme.LIGHT -> getString(stringRes.light)
		Theme.DARK -> getString(stringRes.dark)
		Theme.AMOLED -> getString(stringRes.amoled)
	}
} ?: ""

fun Configuration.getThemeRes(theme: Theme) = when (theme) {
	Theme.SYSTEM -> {
		if ((uiMode and Configuration.UI_MODE_NIGHT_YES) != 0)
			R.style.Theme_Main_Dark else R.style.Theme_Main_Light
	}
	Theme.SYSTEM_BLACK -> {
		if ((uiMode and Configuration.UI_MODE_NIGHT_YES) != 0)
			R.style.Theme_Main_Amoled else R.style.Theme_Main_Light
	}
	Theme.LIGHT -> R.style.Theme_Main_Light
	Theme.DARK -> R.style.Theme_Main_Dark
	Theme.AMOLED -> R.style.Theme_Main_Amoled
}

fun Context?.autoSyncName(autoSync: AutoSync) = this?.let {
	when (autoSync) {
		AutoSync.NEVER -> getString(stringRes.never)
		AutoSync.WIFI_ONLY -> getString(stringRes.only_on_wifi)
		AutoSync.WIFI_PLUGGED_IN -> getString(stringRes.only_on_wifi)
		AutoSync.ALWAYS -> getString(stringRes.always)
	}
} ?: ""

fun Context?.proxyName(proxyType: ProxyType) = this?.let {
	when (proxyType) {
		ProxyType.DIRECT -> getString(stringRes.no_proxy)
		ProxyType.HTTP -> getString(stringRes.http_proxy)
		ProxyType.SOCKS -> getString(stringRes.socks_proxy)
	}
} ?: ""

fun Context?.installerName(installerType: InstallerType) = this?.let {
	when (installerType) {
		InstallerType.LEGACY -> getString(stringRes.legacy_installer)
		InstallerType.SESSION -> getString(stringRes.session_installer)
		InstallerType.SHIZUKU -> getString(stringRes.shizuku_installer)
		InstallerType.ROOT -> getString(stringRes.root_installer)
	}
} ?: ""