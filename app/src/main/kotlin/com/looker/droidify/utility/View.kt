package com.looker.droidify.utility

import android.view.View
import androidx.annotation.IdRes

fun<T: View> View.compatRequireViewById(@IdRes id: Int): T {
    return findViewById(id)!!
}
