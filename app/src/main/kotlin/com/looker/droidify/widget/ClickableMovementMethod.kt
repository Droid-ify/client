package com.looker.droidify.widget

import android.text.Selection
import android.text.Spannable
import android.text.method.MovementMethod
import android.text.style.ClickableSpan
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView

object ClickableMovementMethod : MovementMethod {
    override fun initialize(widget: TextView, text: Spannable) {
        Selection.removeSelection(text)
    }

    override fun onTouchEvent(widget: TextView, text: Spannable, event: MotionEvent): Boolean {
        val action = event.action
        val down = action == MotionEvent.ACTION_DOWN
        val up = action == MotionEvent.ACTION_UP
        return (down || up) && run {
            val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
            val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
            val layout = widget.layout
            val line = layout.getLineForVertical(y)
            val offset = layout.getOffsetForHorizontal(line, x.toFloat())
            val span = text.getSpans(offset, offset, ClickableSpan::class.java)?.firstOrNull()
            if (span != null) {
                if (down) {
                    Selection.setSelection(text, text.getSpanStart(span), text.getSpanEnd(span))
                } else {
                    span.onClick(widget)
                }
                true
            } else {
                Selection.removeSelection(text)
                false
            }
        }
    }

    override fun onKeyDown(
        widget: TextView,
        text: Spannable,
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = false

    override fun onKeyUp(
        widget: TextView,
        text: Spannable,
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = false

    override fun onKeyOther(view: TextView, text: Spannable, event: KeyEvent): Boolean = false
    override fun onTakeFocus(widget: TextView, text: Spannable, direction: Int) = Unit
    override fun onTrackballEvent(widget: TextView, text: Spannable, event: MotionEvent): Boolean =
        false

    override fun onGenericMotionEvent(
        widget: TextView,
        text: Spannable,
        event: MotionEvent,
    ): Boolean = false

    override fun canSelectArbitrarily(): Boolean = false
}
