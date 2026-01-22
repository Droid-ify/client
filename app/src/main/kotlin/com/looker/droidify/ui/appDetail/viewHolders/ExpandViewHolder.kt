package com.looker.droidify.ui.appDetail.viewHolders

import android.view.View
import com.google.android.material.button.MaterialButton
import com.looker.droidify.R
import com.looker.droidify.ui.appDetail.AppDetailAdapter
import com.looker.droidify.ui.appDetail.AppDetailItem
import com.looker.droidify.utility.common.extension.compatRequireViewById

class ExpandViewHolder(
    itemView: View,
    private val onRowClick: OnRowLickListener,
) : BaseViewHolder<AppDetailItem.ExpandItem>(itemView), View.OnClickListener {
    private val button: MaterialButton = itemView.compatRequireViewById(R.id.expand_view_button)

    var isExpanded: Boolean = false

    fun interface OnRowLickListener {
        fun onRowClick(item: AppDetailItem.ExpandItem, position: Int)
    }

    init {
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v == itemView) {
            onRowClick.onRowClick(currentItem, absoluteAdapterPosition)
        }
    }

    override fun bindImpl(item: AppDetailItem.ExpandItem) {
        val context = button.context
        button.text = if (isExpanded) {
            when (item.expandType) {
                AppDetailAdapter.ExpandType.VERSIONS -> context.getString(R.string.show_older_versions)
                else -> context.getString(R.string.show_more)
            }
        } else {
            context.getString(R.string.show_less)
        }
    }
}
