package com.looker.droidify.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.looker.droidify.R
import com.looker.droidify.utility.extension.resources.*
import kotlin.math.*

class DividerItemDecoration(context: Context, private val configure: (context: Context,
  position: Int, configuration: Configuration) -> Unit): RecyclerView.ItemDecoration() {
  interface Configuration {
    fun set(needDivider: Boolean, toTop: Boolean, paddingStart: Int, paddingEnd: Int)
  }

  private class ConfigurationHolder: Configuration {
    var needDivider = false
    var toTop = false
    var paddingStart = 0
    var paddingEnd = 0

    override fun set(needDivider: Boolean, toTop: Boolean, paddingStart: Int, paddingEnd: Int) {
      this.needDivider = needDivider
      this.toTop = toTop
      this.paddingStart = paddingStart
      this.paddingEnd = paddingEnd
    }
  }

  private val View.configuration: ConfigurationHolder
    get() = getTag(R.id.divider_configuration) as? ConfigurationHolder ?: run {
      val configuration = ConfigurationHolder()
      setTag(R.id.divider_configuration, configuration)
      configuration
    }

  private val divider = context.getDrawableFromAttr(android.R.attr.listDivider)
  private val bounds = Rect()

  private fun draw(c: Canvas, configuration: ConfigurationHolder, view: View, top: Int, width: Int, rtl: Boolean) {
    val divider = divider
    val left = if (rtl) configuration.paddingEnd else configuration.paddingStart
    val right = width - (if (rtl) configuration.paddingStart else configuration.paddingEnd)
    val translatedTop = top + view.translationY.roundToInt()
    divider.alpha = (view.alpha * 0xff).toInt()
    divider.setBounds(left, translatedTop, right, translatedTop + divider.intrinsicHeight)
    divider.draw(c)
  }

  override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
    val divider = divider
    val bounds = bounds
    val rtl = parent.layoutDirection == View.LAYOUT_DIRECTION_RTL
    for (i in 0 until parent.childCount) {
      val view = parent.getChildAt(i)
      val configuration = view.configuration
      if (configuration.needDivider) {
        val position = parent.getChildAdapterPosition(view)
        if (position == parent.adapter!!.itemCount - 1) {
          parent.getDecoratedBoundsWithMargins(view, bounds)
          draw(c, configuration, view, bounds.bottom, parent.width, rtl)
        } else {
          val toTopView = if (configuration.toTop && position >= 0)
            parent.findViewHolderForAdapterPosition(position + 1)?.itemView else null
          if (toTopView != null) {
            parent.getDecoratedBoundsWithMargins(toTopView, bounds)
            draw(c, configuration, toTopView, bounds.top - divider.intrinsicHeight, parent.width, rtl)
          } else {
            parent.getDecoratedBoundsWithMargins(view, bounds)
            draw(c, configuration, view, bounds.bottom - divider.intrinsicHeight, parent.width, rtl)
          }
        }
      }
    }
  }

  override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
    val configuration = view.configuration
    val position = parent.getChildAdapterPosition(view)
    if (position >= 0) {
      configure(view.context, position, configuration)
    }
    val needDivider = position < parent.adapter!!.itemCount - 1 && configuration.needDivider
    outRect.set(0, 0, 0, if (needDivider) divider.intrinsicHeight else 0)
  }
}
