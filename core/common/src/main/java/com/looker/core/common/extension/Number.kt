package com.looker.core.common.extension

import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import com.looker.core.common.DataSize
import kotlin.math.roundToInt

infix fun Long.percentBy(denominator: Long?): Int {
    if (denominator == null || denominator < 1) return -1
    return (this * 100 / denominator).toInt()
}

infix fun DataSize.percentBy(denominator: DataSize?): Int = value percentBy denominator?.value

val Number.px
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )

context(View)
val Int.dp: Int
    get() = (this * resources.displayMetrics.density).roundToInt()
