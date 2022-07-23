package com.looker.droidify.utility.extension

import android.content.Context
import android.content.res.Configuration
import com.looker.core_datastore.model.AutoSync
import com.looker.core_datastore.model.InstallerType
import com.looker.core_datastore.model.ProxyType
import com.looker.core_datastore.model.Theme
import com.looker.droidify.R
import com.looker.droidify.content.Preferences

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
		Theme.SYSTEM -> getString(R.string.system)
		Theme.SYSTEM_BLACK -> getString(R.string.system) + " " + getString(R.string.amoled)
		Theme.LIGHT -> getString(R.string.light)
		Theme.DARK -> getString(R.string.dark)
		Theme.AMOLED -> getString(R.string.amoled)
	}
} ?: ""

fun Configuration.getThemeRes(theme: Theme) = when(theme){
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

fun Context?.autoSyncName(autoSync: AutoSync) = this?.let{
	when (autoSync) {
		AutoSync.NEVER -> getString(R.string.never)
		AutoSync.WIFI_ONLY -> getString(R.string.only_on_wifi)
		AutoSync.WIFI_PLUGGED_IN -> getString(R.string.only_on_wifi)
		AutoSync.ALWAYS -> getString(R.string.always)
	}
} ?: ""

fun Context?.proxyName(proxyType: ProxyType) = this?.let{
	when (proxyType) {
		ProxyType.DIRECT -> getString(R.string.no_proxy)
		ProxyType.HTTP -> getString(R.string.http_proxy)
		ProxyType.SOCKS -> getString(R.string.socks_proxy)
	}
} ?: ""

fun Context?.installerName(installerType: InstallerType) = this?.let {
	when (installerType) {
		InstallerType.LEGACY -> getString(R.string.legacy_installer)
		InstallerType.SESSION -> getString(R.string.session_installer)
		InstallerType.SHIZUKU -> getString(R.string.shizuku_installer)
		InstallerType.ROOT -> getString(R.string.root_installer)
	}
} ?: ""