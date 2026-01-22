package com.looker.droidify.ui.appDetail.viewHolders

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.imageview.ShapeableImageView
import com.looker.droidify.R
import com.looker.droidify.ui.appDetail.AppDetailAdapter
import com.looker.droidify.ui.appDetail.AppDetailItem
import com.looker.droidify.utility.common.extension.compatRequireViewById
import com.looker.droidify.utility.common.extension.getMutatedIcon
import kotlin.math.roundToInt

class PermissionsViewHolder(
    itemView: View,
    private val callbacks: AppDetailAdapter.Callbacks,
) : OverlappingViewHolder<AppDetailItem.PermissionsItem>(itemView), View.OnClickListener {
    companion object {
        private val measurement: Measurement<Int> = Measurement()
    }

    private val icon: ShapeableImageView = itemView.compatRequireViewById(R.id.icon)
    private val text: TextView = itemView.compatRequireViewById(R.id.text)

    init {
        val margin = measurement.invalidate(itemView.resources) {
            @SuppressLint("SetTextI18n")
            text.text = "measure"
            measurement.measure(itemView)
            ((itemView.measuredHeight - icon.measuredHeight) / 2f).roundToInt()
        }
        (icon.layoutParams as ViewGroup.MarginLayoutParams).apply {
            topMargin += margin
            bottomMargin += margin
        }

        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v == itemView) {
            val permissionsItem = currentItem
            callbacks.onPermissionsClick(
                group = permissionsItem.group?.name,
                permissions = permissionsItem.permissionNames,
            )
        }
    }

    override fun bindImpl(item: AppDetailItem.PermissionsItem) {
        val context = itemView.context
        val packageManager = context.packageManager

        val group = item.group
        icon.setImageDrawable(
            if (group != null && group.icon != 0) {
                group.loadUnbadgedIcon(packageManager)
            } else {
                null
            } ?: context.getMutatedIcon(R.drawable.ic_perm_device_information),
        )

        text.text = item.formattedText
    }
}
