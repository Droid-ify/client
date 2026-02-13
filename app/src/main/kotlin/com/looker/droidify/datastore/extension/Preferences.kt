package com.looker.droidify.datastore.extension

import android.content.Context
import android.content.res.Configuration
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.datastore.model.Theme
import com.looker.droidify.utility.common.SdkCheck
import com.looker.droidify.R.string as stringRes
import com.looker.droidify.R.style as styleRes

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

fun Context?.sortOrderName(sortOrder: SortOrder) = this?.let {
    when (sortOrder) {
        SortOrder.UPDATED -> getString(stringRes.recently_updated)
        SortOrder.ADDED -> getString(stringRes.whats_new)
        SortOrder.NAME -> getString(stringRes.name)
        SortOrder.SIZE -> getString(stringRes.size)
    }
} ?: ""
