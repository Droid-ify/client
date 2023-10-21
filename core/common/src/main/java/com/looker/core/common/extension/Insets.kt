package com.looker.core.common.extension

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
import com.looker.core.common.SdkCheck
import com.looker.core.common.extension.InsetSides.BOTTOM
import com.looker.core.common.extension.InsetSides.LEFT
import com.looker.core.common.extension.InsetSides.RIGHT
import com.looker.core.common.extension.InsetSides.TOP

fun View.systemBarsMargin(
    persistentPadding: Int,
    allowedSides: List<InsetSides> = listOf(LEFT, RIGHT, BOTTOM)
) {
    if (SdkCheck.isR) {
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (TOP in allowedSides) topMargin = insets.top + marginTop
                if (LEFT in allowedSides) leftMargin = insets.left + marginLeft
                if (BOTTOM in allowedSides) bottomMargin = insets.bottom + persistentPadding
                if (RIGHT in allowedSides) rightMargin = insets.right + persistentPadding
            }
            WindowInsetsCompat.CONSUMED
        }
    }
}

fun RecyclerView.systemBarsPadding(
    allowedSides: List<InsetSides> = listOf(LEFT, RIGHT, BOTTOM),
    includeFab: Boolean = true
) {
    if (SdkCheck.isR) {
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
            clipToPadding = false
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                if (LEFT in allowedSides) insets.left else 0,
                if (TOP in allowedSides) insets.top else 0,
                if (RIGHT in allowedSides) insets.right else 0,
                if (BOTTOM in allowedSides) {
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
    allowedSides: List<InsetSides> = listOf(LEFT, RIGHT, BOTTOM)
) {
    if (SdkCheck.isR) {
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
            clipToPadding = false
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                if (LEFT in allowedSides) insets.left else 0,
                if (TOP in allowedSides) insets.top else 0,
                if (RIGHT in allowedSides) insets.right else 0,
                if (BOTTOM in allowedSides) insets.bottom else 0
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
