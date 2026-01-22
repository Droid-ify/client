package com.looker.droidify.ui.appDetail.viewHolders

import android.content.res.ColorStateList
import android.view.View
import com.google.android.material.button.MaterialButton
import com.looker.droidify.R
import com.looker.droidify.ui.appDetail.AppDetailAdapter
import com.looker.droidify.ui.appDetail.AppDetailItem
import com.looker.droidify.utility.common.extension.compatRequireViewById
import com.looker.droidify.utility.common.extension.getColorFromAttr
import com.looker.droidify.utility.common.extension.getDrawableCompat
import com.looker.droidify.utility.extension.resources.sizeScaled

class InstallButtonViewHolder(
    itemView: View,
    private val callbacks: AppDetailAdapter.Callbacks,
) : BaseViewHolder<AppDetailItem.InstallButtonItem>(itemView), View.OnClickListener {
    private val button: MaterialButton = itemView.compatRequireViewById(R.id.action)

    private val actionTintNormal: ColorStateList = button.context.getColorFromAttr(com.google.android.material.R.attr.colorPrimary)
    private val actionTintOnNormal: ColorStateList = button.context.getColorFromAttr(com.google.android.material.R.attr.colorOnPrimary)
    private val actionTintCancel: ColorStateList = button.context.getColorFromAttr(com.google.android.material.R.attr.colorError)
    private val actionTintOnCancel: ColorStateList = button.context.getColorFromAttr(com.google.android.material.R.attr.colorOnError)
    private val actionTintDisabled: ColorStateList = button.context.getColorFromAttr(com.google.android.material.R.attr.colorOutline)
    private val actionTintOnDisabled: ColorStateList = button.context.getColorFromAttr(android.R.attr.colorBackground)

    init {
        button.height = itemView.resources.sizeScaled(48)
        button.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v == button) {
            currentItem.action?.let(callbacks::onActionClick)
        }
    }

    override fun bindImpl(item: AppDetailItem.InstallButtonItem) {
        val action = item.action
        button.apply {
            isEnabled = action != null
            if (action != null) {
                icon = context.getDrawableCompat(action.iconResId)
                setText(action.titleResId)
                setTextColor(
                    if (action == AppDetailAdapter.Action.CANCEL) {
                        actionTintOnCancel
                    } else {
                        actionTintOnNormal
                    },
                )
                backgroundTintList = if (action == AppDetailAdapter.Action.CANCEL) {
                    actionTintCancel
                } else {
                    actionTintNormal
                }
                iconTint = if (action == AppDetailAdapter.Action.CANCEL) {
                    actionTintOnCancel
                } else {
                    actionTintOnNormal
                }
            } else {
                icon = context.getDrawableCompat(R.drawable.ic_cancel)
                setText(R.string.cancel)
                setTextColor(actionTintOnDisabled)
                backgroundTintList = actionTintDisabled
                iconTint = actionTintOnDisabled
            }
        }
    }
}
