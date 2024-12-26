package com.looker.core.common.extension

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.request.ImageRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.min
import kotlin.math.roundToInt

fun ImageRequest.Builder.authentication(base64: String) {
    addHeader("Authorization", base64)
}

fun TextView.setTextSizeScaled(size: Int) {
    val realSize = (size * resources.displayMetrics.scaledDensity).roundToInt()
    setTextSize(TypedValue.COMPLEX_UNIT_PX, realSize.toFloat())
}

fun ViewGroup.inflate(layoutResId: Int): View {
    return LayoutInflater.from(context).inflate(layoutResId, this, false)
}

val RecyclerView.firstItemPosition: Flow<Int>
    get() = callbackFlow {
        val listener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val position = (recyclerView.layoutManager as LinearLayoutManager)
                    .findFirstVisibleItemPosition()
                trySend(position)
            }
        }
        addOnScrollListener(listener)
        awaitClose { removeOnScrollListener(listener) }
    }.distinctUntilChanged().conflate()

val RecyclerView.isFirstItemVisible: Flow<Boolean>
    get() = firstItemPosition.map { it == 0 }.distinctUntilChanged()

val View.minDimension: Int
    get() = (
        min(
            layoutParams.width,
            layoutParams.height
        ) / resources.displayMetrics.density
        ).roundToInt()

val View.dpi: Int
    get() = (context.resources.displayMetrics.densityDpi * minDimension) / 48
