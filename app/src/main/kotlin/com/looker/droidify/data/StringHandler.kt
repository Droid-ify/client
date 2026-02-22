package com.looker.droidify.data

import android.content.Context
import android.content.res.Resources

class StringHandler(context: Context) {

    private val resources: Resources = context.resources

    fun getString(id: Int): String {
        resources.configuration
        return resources.getString(id)
    }
}
