package com.looker.core.common.extension

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlin.math.roundToInt

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
				val position =
					(recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
				trySend(position)
			}
		}
		addOnScrollListener(listener)
		awaitClose { removeOnScrollListener(listener) }
	}.distinctUntilChanged().conflate()

val RecyclerView.firstItemVisible: Flow<Boolean>
	get() = callbackFlow {
		val listener = object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				val position =
					(recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
				trySend(position == 0)
			}
		}
		addOnScrollListener(listener)
		awaitClose { removeOnScrollListener(listener) }
	}.distinctUntilChanged().conflate()