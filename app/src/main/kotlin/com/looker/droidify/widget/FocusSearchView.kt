package com.looker.droidify.widget

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.SearchView

class FocusSearchView : SearchView {
	constructor(context: Context) : super(context)
	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
		context,
		attrs,
		defStyleAttr
	)

	var allowFocus = true

	override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
		// Always clear focus on back press
		return if (hasFocus() && event.keyCode == KeyEvent.KEYCODE_BACK) {
			if (event.action == KeyEvent.ACTION_UP) {
				clearFocus()
			}
			true
		} else {
			super.dispatchKeyEventPreIme(event)
		}
	}

	override fun setIconified(iconify: Boolean) {
		super.setIconified(iconify)

		// Don't focus view and raise keyboard unless allowed
		if (!iconify && !allowFocus) {
			clearFocus()
		}
	}
}
