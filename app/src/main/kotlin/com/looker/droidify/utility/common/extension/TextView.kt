package com.looker.droidify.utility.common.extension

import android.util.TypedValue
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.PrecomputedTextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private val sExecutor: ExecutorService = Executors.newFixedThreadPool(4)!!

fun AppCompatTextView.submitText(str: CharSequence?) {
    if (str == null) {
        text = null
        return
    }

    val textCompatFuture = PrecomputedTextCompat.getTextFuture(
        /* charSequence = */ str,
        /* params = */ textMetricsParamsCompat,
        /* executor = */ sExecutor
    )

    setTextFuture(textCompatFuture)
}

fun TextView.setTextSizeScaled(size: Int) {
    val realSize = (size * resources.displayMetrics.scaledDensity).roundToInt()
    setTextSize(TypedValue.COMPLEX_UNIT_PX, realSize.toFloat())
}
