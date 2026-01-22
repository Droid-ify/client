package com.looker.droidify.ui.appDetail.viewHolders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.looker.droidify.ui.appDetail.AppDetailItem

abstract class BaseViewHolder<T: AppDetailItem>(
    itemView: View
): RecyclerView.ViewHolder(itemView) {
    lateinit var currentItem: T

    fun bind(item: T) {
        currentItem = item
        bindImpl(item)
    }

    protected abstract fun bindImpl(item: T)
}
