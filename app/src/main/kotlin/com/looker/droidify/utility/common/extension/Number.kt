package com.looker.droidify.utility.common.extension

import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import kotlin.math.roundToInt

val Number.dpToPx
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )

context(view: View)
val Int.dp: Int
    get() = (this * view.resources.displayMetrics.density).roundToInt()
