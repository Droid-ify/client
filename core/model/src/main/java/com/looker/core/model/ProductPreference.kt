package com.looker.core.model

data class ProductPreference(val ignoreUpdates: Boolean, val ignoreVersionCode: Long) {
    fun shouldIgnoreUpdate(versionCode: Long): Boolean {
        return ignoreUpdates || ignoreVersionCode == versionCode
    }
}
