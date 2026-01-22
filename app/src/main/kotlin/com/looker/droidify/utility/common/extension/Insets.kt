package com.looker.droidify.utility.common.extension

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.looker.droidify.utility.common.SdkCheck

fun View.systemBarsMargin(
    persistentPadding: Int,
    allowedSides: List<InsetSides> = listOf(InsetSides.LEFT, InsetSides.RIGHT, InsetSides.BOTTOM)
) {
    if (SdkCheck.isR) {
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (InsetSides.TOP in allowedSides) topMargin = insets.top + marginTop
                if (InsetSides.LEFT in allowedSides) leftMargin = insets.left + marginLeft
                if (InsetSides.BOTTOM in allowedSides) bottomMargin = insets.bottom + persistentPadding
                if (InsetSides.RIGHT in allowedSides) rightMargin = insets.right + persistentPadding
            }
            WindowInsetsCompat.CONSUMED
        }
    }
}

fun RecyclerView.systemBarsPadding(
    allowedSides: List<InsetSides> = listOf(InsetSides.LEFT, InsetSides.RIGHT, InsetSides.BOTTOM),
    includeFab: Boolean = true
) {
    if (SdkCheck.isR) {
        val defaultPaddingLeft = paddingLeft
        val defaultTopPadding = paddingTop
        val defaultBottomPadding = paddingBottom
        val defaultRightPadding = paddingRight

        ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
            clipToPadding = false
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = defaultPaddingLeft + if (InsetSides.LEFT in allowedSides) insets.left else 0,
                top = defaultTopPadding + if (InsetSides.TOP in allowedSides) insets.top else 0,
                right = defaultRightPadding + if (InsetSides.RIGHT in allowedSides) insets.right else 0,
                bottom = defaultBottomPadding + if (InsetSides.BOTTOM in allowedSides) {
                    insets.bottom + if (includeFab) 88.dp else 0
                } else {
                    0
                }
            )
            WindowInsetsCompat.CONSUMED
        }
    }
}

fun NestedScrollView.systemBarsPadding(
    allowedSides: List<InsetSides> = listOf(InsetSides.LEFT, InsetSides.RIGHT, InsetSides.BOTTOM)
) {
    if (SdkCheck.isR) {
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
            clipToPadding = false
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                if (InsetSides.LEFT in allowedSides) insets.left else 0,
                if (InsetSides.TOP in allowedSides) insets.top else 0,
                if (InsetSides.RIGHT in allowedSides) insets.right else 0,
                if (InsetSides.BOTTOM in allowedSides) insets.bottom else 0
            )
            WindowInsetsCompat.CONSUMED
        }
    }
}

enum class InsetSides {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM
}
