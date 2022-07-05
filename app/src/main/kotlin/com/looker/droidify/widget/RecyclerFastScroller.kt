package com.looker.droidify.widget

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.looker.droidify.utility.extension.resources.getDrawableFromAttr
import com.looker.droidify.utility.extension.resources.sizeScaled
import kotlin.math.max
import kotlin.math.roundToInt

@SuppressLint("ClickableViewAccessibility")
class RecyclerFastScroller(private val recyclerView: RecyclerView) {
	companion object {
		private const val TRANSITION_IN = 100L
		private const val TRANSITION_OUT = 200L
		private const val TRANSITION_OUT_DELAY = 1000L

		private val stateNormal = intArrayOf()
		private val statePressed = intArrayOf(android.R.attr.state_pressed)
	}

	private val thumbDrawable =
		recyclerView.context.getDrawableFromAttr(android.R.attr.fastScrollThumbDrawable)
	private val trackDrawable =
		recyclerView.context.getDrawableFromAttr(android.R.attr.fastScrollTrackDrawable)
	private val minTrackSize = recyclerView.resources.sizeScaled(16)

	private data class FastScrolling(
		val startAtThumbOffset: Float?,
		val startY: Float,
		val currentY: Float,
	)

	private var scrolling = false
	private var fastScrolling: FastScrolling? = null
	private var display = Pair(0L, false)

	private val invalidateTransition = Runnable(recyclerView::invalidate)

	private fun updateState(scrolling: Boolean, fastScrolling: FastScrolling?) {
		val oldDisplay = this.scrolling || this.fastScrolling != null
		val newDisplay = scrolling || fastScrolling != null
		this.scrolling = scrolling
		this.fastScrolling = fastScrolling
		if (oldDisplay != newDisplay) {
			recyclerView.removeCallbacks(invalidateTransition)
			val time = SystemClock.elapsedRealtime()
			val passed = time - display.first
			val start = if (newDisplay && passed < (TRANSITION_OUT + TRANSITION_OUT_DELAY)) {
				if (passed <= TRANSITION_OUT_DELAY) {
					0L
				} else {
					time - ((TRANSITION_OUT_DELAY + TRANSITION_OUT - passed).toFloat() /
							TRANSITION_OUT * TRANSITION_IN).toLong()
				}
			} else if (!newDisplay && passed < TRANSITION_IN) {
				time - ((TRANSITION_IN - passed).toFloat() / TRANSITION_IN *
						TRANSITION_OUT).toLong() - TRANSITION_OUT_DELAY
			} else {
				if (!newDisplay) {
					recyclerView.postDelayed(invalidateTransition, TRANSITION_OUT_DELAY)
				}
				time
			}
			display = Pair(start, newDisplay)
			recyclerView.invalidate()
		}
	}

	private val scrollListener = object : RecyclerView.OnScrollListener() {
		override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
			updateState(newState != RecyclerView.SCROLL_STATE_IDLE, fastScrolling)
		}

		override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
			if (fastScrolling == null) {
				recyclerView.invalidate()
			}
		}
	}

	private inline fun withScroll(callback: (itemHeight: Int, thumbHeight: Int, range: Int) -> Unit): Boolean {
		val count = recyclerView.adapter?.itemCount ?: 0
		return count > 0 && run {
			val itemHeight = Rect().apply {
				recyclerView
					.getDecoratedBoundsWithMargins(recyclerView.getChildAt(0), this)
			}.height()
			val scrollCount = count - recyclerView.height / itemHeight
			scrollCount > 0 && run {
				val range = count * itemHeight
				val thumbHeight = max(
					recyclerView.height * recyclerView.height / range,
					thumbDrawable.intrinsicHeight
				)
				range >= recyclerView.height * 2 && run {
					callback(itemHeight, thumbHeight, range)
					true
				}
			}
		}
	}

	private fun calculateOffset(thumbHeight: Int, fastScrolling: FastScrolling): Float {
		return if (fastScrolling.startAtThumbOffset != null) {
			(fastScrolling.startAtThumbOffset + (fastScrolling.currentY - fastScrolling.startY) /
					(recyclerView.height - thumbHeight)).coerceIn(0f, 1f)
		} else {
			((fastScrolling.currentY - thumbHeight / 2f) / (recyclerView.height - thumbHeight)).coerceIn(
				0f,
				1f
			)
		}
	}

	private fun currentOffset(itemHeight: Int, range: Int): Float {
		val view = recyclerView.getChildAt(0)
		val position = recyclerView.getChildAdapterPosition(view)
		val positionOffset = -view.top
		val scrollPosition = position * itemHeight + positionOffset
		return scrollPosition.toFloat() / (range - recyclerView.height)
	}

	private fun scroll(
		itemHeight: Int,
		thumbHeight: Int,
		range: Int,
		fastScrolling: FastScrolling,
	) {
		val offset = calculateOffset(thumbHeight, fastScrolling)
		val scrollPosition = ((range - recyclerView.height) * offset).roundToInt()
		val position = scrollPosition / itemHeight
		val positionOffset = scrollPosition - position * itemHeight
		val layoutManager = recyclerView.layoutManager as LinearLayoutManager
		layoutManager.scrollToPositionWithOffset(position, -positionOffset)
	}

	private val touchListener = object : RecyclerView.OnItemTouchListener {
		private var disallowIntercept = false

		private fun handleTouchEvent(event: MotionEvent, intercept: Boolean): Boolean {
			val recyclerView = recyclerView
			val lastFastScrolling = fastScrolling
			return when {
				intercept && disallowIntercept -> {
					false
				}
				event.action == MotionEvent.ACTION_DOWN -> {
					val rtl = recyclerView.layoutDirection == RecyclerView.LAYOUT_DIRECTION_RTL
					val trackWidth = max(
						minTrackSize,
						max(thumbDrawable.intrinsicWidth, trackDrawable.intrinsicWidth)
					)
					val atThumbVertical =
						if (rtl) event.x <= trackWidth else event.x >= recyclerView.width - trackWidth
					atThumbVertical && run {
						withScroll { itemHeight, thumbHeight, range ->
							(recyclerView.parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(
								true
							)
							val offset = currentOffset(itemHeight, range)
							val thumbY = ((recyclerView.height - thumbHeight) * offset).roundToInt()
							val atThumb = event.y >= thumbY && event.y <= thumbY + thumbHeight
							val fastScrolling =
								FastScrolling(if (atThumb) offset else null, event.y, event.y)
							scroll(itemHeight, thumbHeight, range, fastScrolling)
							updateState(scrolling, fastScrolling)
							recyclerView.invalidate()
						}
					}
				}
				else -> lastFastScrolling != null && run {
					val success = withScroll { itemHeight, thumbHeight, range ->
						val fastScrolling = lastFastScrolling.copy(currentY = event.y)
						scroll(itemHeight, thumbHeight, range, fastScrolling)
						updateState(scrolling, fastScrolling)
						recyclerView.invalidate()
					}
					val cancel =
						event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL
					if (!success || cancel) {
						(recyclerView.parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(
							false
						)
						updateState(scrolling, null)
						recyclerView.invalidate()
					}
					true
				}
			}
		}

		override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
			return handleTouchEvent(e, true)
		}

		override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
			handleTouchEvent(e, false)
		}

		override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
			this.disallowIntercept = disallowIntercept
			if (disallowIntercept && fastScrolling != null) {
				updateState(scrolling, null)
				recyclerView.invalidate()
			}
		}
	}

	private fun handleDraw(canvas: Canvas) {
		withScroll { itemHeight, thumbHeight, range ->
			val display = display
			val time = SystemClock.elapsedRealtime()
			val passed = time - display.first
			val shouldInvalidate = display.second && passed < TRANSITION_IN ||
					!display.second && passed >= TRANSITION_OUT_DELAY && passed < TRANSITION_OUT_DELAY + TRANSITION_OUT
			val stateValue = (if (display.second) {
				passed.toFloat() / TRANSITION_IN
			} else {
				1f - (passed - TRANSITION_OUT_DELAY).toFloat() / TRANSITION_OUT
			}).coerceIn(0f, 1f)

			if (stateValue > 0f) {
				val rtl = recyclerView.layoutDirection == RecyclerView.LAYOUT_DIRECTION_RTL
				val thumbDrawable = thumbDrawable
				val trackDrawable = trackDrawable
				val maxWidth = max(thumbDrawable.intrinsicWidth, trackDrawable.intrinsicHeight)
				val translateX = (maxWidth * (1f - stateValue)).roundToInt()
				val fastScrolling = fastScrolling

				val scrollValue = (if (fastScrolling != null) {
					calculateOffset(thumbHeight, fastScrolling)
				} else {
					currentOffset(itemHeight, range)
				}).coerceIn(0f, 1f)
				val thumbY = ((recyclerView.height - thumbHeight) * scrollValue).roundToInt()

				trackDrawable.state = if (fastScrolling != null) statePressed else stateNormal
				val trackExtra = (maxWidth - trackDrawable.intrinsicWidth) / 2
				if (rtl) {
					trackDrawable.setBounds(
						trackExtra - translateX, 0,
						trackExtra + trackDrawable.intrinsicWidth - translateX, recyclerView.height
					)
				} else {
					trackDrawable.setBounds(
						recyclerView.width - trackExtra - trackDrawable.intrinsicWidth + translateX,
						0, recyclerView.width - trackExtra + translateX, recyclerView.height
					)
				}
				trackDrawable.draw(canvas)
				val thumbExtra = (maxWidth - thumbDrawable.intrinsicWidth) / 2
				thumbDrawable.state = if (fastScrolling != null) statePressed else stateNormal
				if (rtl) {
					thumbDrawable.setBounds(
						thumbExtra - translateX, thumbY,
						thumbExtra + thumbDrawable.intrinsicWidth - translateX, thumbY + thumbHeight
					)
				} else {
					thumbDrawable.setBounds(
						recyclerView.width - thumbExtra - thumbDrawable.intrinsicWidth + translateX,
						thumbY, recyclerView.width - thumbExtra + translateX, thumbY + thumbHeight
					)
				}
				thumbDrawable.draw(canvas)
			}

			if (shouldInvalidate) {
				recyclerView.invalidate()
			}
		}
	}

	init {
		recyclerView.addOnScrollListener(scrollListener)
		recyclerView.addOnItemTouchListener(touchListener)
		recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
			override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) =
				handleDraw(c)
		})
	}
}
