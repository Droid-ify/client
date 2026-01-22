package com.looker.droidify.ui.appDetail.viewHolders

import android.view.View
import com.google.android.material.materialswitch.MaterialSwitch
import com.looker.droidify.R
import com.looker.droidify.content.ProductPreferences
import com.looker.droidify.ui.appDetail.AppDetailAdapter
import com.looker.droidify.ui.appDetail.AppDetailItem
import com.looker.droidify.ui.appDetail.SwitchType
import com.looker.droidify.utility.common.extension.compatRequireViewById

class SwitchViewHolder(
    itemView: View,
    private val callbacks: AppDetailAdapter.Callbacks,
) : BaseViewHolder<AppDetailItem.SwitchItem>(itemView), View.OnClickListener {
    private val switch: MaterialSwitch = itemView.compatRequireViewById(R.id.update_state_switch)

    private val statefulViews: Array<View>
        get() = arrayOf(itemView, switch)

    init {
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v == itemView) {
            val switchItem = currentItem
            val packageName = switchItem.packageName

            val oldProductPreference = ProductPreferences[packageName]
            val productPreference = when (switchItem.switchType) {
                SwitchType.IGNORE_ALL_UPDATES -> {
                    oldProductPreference.copy(
                        ignoreUpdates = !oldProductPreference.ignoreUpdates,
                    )
                }

                SwitchType.IGNORE_THIS_UPDATE -> {
                    oldProductPreference.copy(
                        ignoreVersionCode =
                            if (oldProductPreference.ignoreVersionCode == switchItem.versionCode) {
                                0
                            } else {
                                switchItem.versionCode
                            },
                    )
                }
            }
            ProductPreferences[packageName] = productPreference

            callbacks.onPreferenceChanged(productPreference)
        }
    }

    override fun bindImpl(item: AppDetailItem.SwitchItem) {
        val productPreference = ProductPreferences[item.packageName]
        val ignoreUpdates = productPreference.ignoreUpdates

        val checked: Boolean
        val enabled: Boolean
        when (item.switchType) {
            SwitchType.IGNORE_ALL_UPDATES -> {
                checked = ignoreUpdates
                enabled = true
            }

            SwitchType.IGNORE_THIS_UPDATE -> {
                checked = ignoreUpdates || productPreference.ignoreVersionCode == item.versionCode
                enabled = !ignoreUpdates
            }
        }

        switch.setText(item.switchType.titleResId)
        switch.isChecked = checked
        statefulViews.forEach { it.isEnabled = enabled }
    }
}
