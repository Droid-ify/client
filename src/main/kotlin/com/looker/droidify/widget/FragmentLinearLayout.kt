package com.looker.droidify.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

class FragmentLinearLayout : LinearLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        fitsSystemWindows = true
    }

    @Suppress("unused")
    var percentTranslationY: Float
        get() = height.let { if (it > 0) translationY / it else 0f }
        set(value) {
            translationY = value * height
        }
}
