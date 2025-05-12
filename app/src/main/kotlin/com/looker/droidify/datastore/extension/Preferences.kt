package com.looker.droidify.datastore.extension

import android.content.Context
import android.content.res.Configuration
import com.looker.droidify.R
import com.looker.droidify.R.string as stringRes
import com.looker.droidify.R.style as styleRes
import com.looker.droidify.utility.common.SdkCheck
import com.looker.droidify.datastore.model.AutoSync
import com.looker.droidify.datastore.model.InstallerType
import com.looker.droidify.datastore.model.ProxyType
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.datastore.model.Theme
import kotlin.time.Duration

fun Configuration.getThemeRes(theme: Theme, dynamicTheme: Boolean) = when (theme) {
    Theme.SYSTEM -> {
        if ((uiMode and Configuration.UI_MODE_NIGHT_YES) != 0) {
            if (SdkCheck.isSnowCake && dynamicTheme) {
                styleRes.Theme_Main_DynamicDark
            } else {
                styleRes.Theme_Main_Dark
            }
        } else {
            if (SdkCheck.isSnowCake && dynamicTheme) {
                styleRes.Theme_Main_DynamicLight
            } else {
                styleRes.Theme_Main_Light
            }
        }
    }

    Theme.SYSTEM_BLACK -> {
        if ((uiMode and Configuration.UI_MODE_NIGHT_YES) != 0) {
            if (SdkCheck.isSnowCake && dynamicTheme) {
                styleRes.Theme_Main_DynamicAmoled
            } else {
                styleRes.Theme_Main_Amoled
            }
        } else {
            if (SdkCheck.isSnowCake && dynamicTheme) {
                styleRes.Theme_Main_DynamicLight
            } else {
                styleRes.Theme_Main_Light
            }
        }
    }

    Theme.LIGHT -> if (SdkCheck.isSnowCake && dynamicTheme) {
        styleRes.Theme_Main_DynamicLight
    } else {
        styleRes.Theme_Main_Light
    }
    Theme.DARK -> if (SdkCheck.isSnowCake && dynamicTheme) {
        styleRes.Theme_Main_DynamicDark
    } else {
        styleRes.Theme_Main_Dark
    }
    Theme.AMOLED -> if (SdkCheck.isSnowCake && dynamicTheme) {
        styleRes.Theme_Main_DynamicAmoled
    } else {
        styleRes.Theme_Main_Amoled
    }
}

fun Context?.toTime(duration: Duration): String {
    val time = duration.inWholeHours.toInt()
    val days = duration.inWholeDays.toInt()
    if (duration == Duration.INFINITE) return this?.getString(stringRes.never) ?: ""
    return if (time >= 24) {
        "$days " + this?.resources?.getQuantityString(
            R.plurals.days,
            days
        )
    } else {
        "$time " + this?.resources?.getQuantityString(R.plurals.hours, time)
    }
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

fun Context?.sortOrderName(sortOrder: SortOrder) = this?.let {
    when (sortOrder) {
        SortOrder.UPDATED -> getString(stringRes.recently_updated)
        SortOrder.ADDED -> getString(stringRes.whats_new)
        SortOrder.NAME -> getString(stringRes.name)
 		SortOrder.SIZE -> getString(stringRes.size)
    }
} ?: ""

fun Context?.autoSyncName(autoSync: AutoSync) = this?.let {
    when (autoSync) {
        AutoSync.NEVER -> getString(stringRes.never)
        AutoSync.WIFI_ONLY -> getString(stringRes.only_on_wifi)
        AutoSync.WIFI_PLUGGED_IN -> getString(stringRes.only_on_wifi_with_charging)
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
