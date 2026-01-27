package com.looker.droidify.utility.common.extension

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.min
import kotlin.math.roundToInt
import com.looker.droidify.R.string as stringRes

private val networkHeader by lazy { NetworkHeaders.Builder() }

fun ImageRequest.Builder.authentication(base64: String) {
    if (base64.isNotEmpty()) {
        networkHeader["Authorization"] = base64
        httpHeaders(networkHeader.build())
    }
}

fun ViewGroup.inflate(layoutResId: Int): View {
    return LayoutInflater.from(context).inflate(layoutResId, this, false)
}

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

fun<T: View> View.compatRequireViewById(@IdRes id: Int): T {
    return findViewById(id)!!
}

fun View.copyLinkToClipboard(link: String) {
    context.copyToClipboard(link)
    Snackbar.make(this, stringRes.link_copied_to_clipboard, Snackbar.LENGTH_SHORT).show()
}
