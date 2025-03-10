package com.looker.droidify.utility.common.extension

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.min
import kotlin.math.roundToInt

private val networkHeader by lazy { NetworkHeaders.Builder() }

fun ImageRequest.Builder.authentication(base64: String) {
    if (base64.isNotEmpty()) {
        networkHeader["Authorization"] = base64
        httpHeaders(networkHeader.build())
    }
}

fun TextView.setTextSizeScaled(size: Int) {
    val realSize = (size * resources.displayMetrics.scaledDensity).roundToInt()
    setTextSize(TypedValue.COMPLEX_UNIT_PX, realSize.toFloat())
}

val Context.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(this)

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
