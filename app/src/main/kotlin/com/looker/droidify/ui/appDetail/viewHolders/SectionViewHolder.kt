package com.looker.droidify.ui.appDetail.viewHolders

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import coil3.load
import com.google.android.material.imageview.ShapeableImageView
import com.looker.droidify.R
import com.looker.droidify.ui.appDetail.AppDetailItem
import com.looker.droidify.ui.appDetail.SectionType
import com.looker.droidify.utility.common.SdkCheck
import com.looker.droidify.utility.common.extension.compatRequireViewById
import com.looker.droidify.utility.common.extension.getColorFromAttr

class SectionViewHolder(
    itemView: View,
    private val onRowClick: OnRowLickListener,
) : BaseViewHolder<AppDetailItem.SectionItem>(itemView), View.OnClickListener {
    private val title: TextView = itemView.compatRequireViewById(R.id.title)
    private val icon: ShapeableImageView = itemView.compatRequireViewById(R.id.icon)

    fun interface OnRowLickListener {
        fun onRowClick(item: AppDetailItem.SectionItem, position: Int)
    }

    private val defaultItemViewPaddingBottom: Int = itemView.paddingBottom

    init {
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v == itemView) {
            onRowClick.onRowClick(currentItem, absoluteAdapterPosition)
        }
    }

    override fun bindImpl(item: AppDetailItem.SectionItem) {
        val context = icon.context
        val sectionType = item.sectionType

        val expandable = item.items.isNotEmpty() || item.collapseCount > 0
        itemView.apply {
            isEnabled = expandable
            updatePadding(
                bottom = if (expandable) defaultItemViewPaddingBottom else 0,
            )
        }

        val color = context.getColorFromAttr(sectionType.colorAttrResId)
        title.apply {
            setTextColor(color)
            text = context.getString(sectionType.titleResId)
        }

        icon.apply {
            val iconRes: Int
            val tText: String?

            if (sectionType == SectionType.VERSIONS) {
                iconRes = R.drawable.ic_question_mark
                tText = context.getString(R.string.rb_badge_info)
            } else {
                iconRes = R.drawable.ic_arrow_down
                tText = null
            }

            load(iconRes)
            isVisible = expandable || sectionType == SectionType.VERSIONS
            scaleY = if (item.collapseCount > 0) -1f else 1f
            imageTintList = color

            if (SdkCheck.isOreo) {
                tooltipText = tText
            }
        }
    }
}
