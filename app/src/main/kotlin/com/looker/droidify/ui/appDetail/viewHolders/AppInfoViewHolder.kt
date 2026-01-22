package com.looker.droidify.ui.appDetail.viewHolders

import android.view.View
import android.widget.LinearLayout
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isGone
import coil3.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.imageview.ShapeableImageView
import com.looker.droidify.R
import com.looker.droidify.ui.appDetail.AppDetailAdapter
import com.looker.droidify.ui.appDetail.AppDetailItem
import com.looker.droidify.utility.common.extension.authentication
import com.looker.droidify.utility.common.extension.compatRequireViewById
import com.looker.droidify.utility.common.extension.corneredBackground
import com.looker.droidify.utility.common.extension.dp
import com.looker.droidify.utility.common.extension.getColorFromAttr

class AppInfoViewHolder(
    itemView: View,
    private val callbacks: AppDetailAdapter.Callbacks,
) : BaseViewHolder<AppDetailItem.AppInfoItem>(itemView), View.OnClickListener {

    private val icon: ShapeableImageView = itemView.compatRequireViewById(R.id.app_icon)
    private val name: TextView = itemView.compatRequireViewById(R.id.app_name)
    private val authorName: TextView = itemView.compatRequireViewById(R.id.author_name)
    private val packageName: TextView = itemView.compatRequireViewById(R.id.package_name)
    private val textSwitcher: TextSwitcher = itemView.compatRequireViewById(R.id.author_package_name)

    private val version: TextView = itemView.compatRequireViewById(R.id.version)
    private val size: TextView = itemView.compatRequireViewById(R.id.size)
    private val downloadsBlockDividier: MaterialDivider = itemView.compatRequireViewById(R.id.downloads_block_divider)
    private val downloadsBlock: LinearLayout = itemView.compatRequireViewById(R.id.downloads_block)
    private val downloads: TextView = itemView.compatRequireViewById(R.id.downloads)

    private val favouriteButton: MaterialButton = itemView.compatRequireViewById(R.id.favourite)

    init {
        val context = itemView.context!!
        textSwitcher.setInAnimation(context, R.anim.slide_right_fade_in)
        textSwitcher.setOutAnimation(context, R.anim.slide_right_fade_out)
        favouriteButton.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v == favouriteButton) {
            callbacks.onFavouriteClicked()
        }
    }

    override fun bindImpl(item: AppDetailItem.AppInfoItem) {
        val product = item.product
        val author = product.author
        var showAuthor = author.name.isNotEmpty()
        val iconUrl = product.item().icon(view = icon, repository = item.repository)
        icon.load(iconUrl) {
            authentication(item.repository.authentication)
        }
        val authorText = if (showAuthor) {
            buildSpannedString {
                append("by ")
                bold { append(author.name) }
            }
        } else {
            buildSpannedString { bold { append(product.packageName) } }
        }
        authorName.text = authorText
        packageName.text = authorText
        if (author.name.isNotEmpty()) {
            icon.setOnClickListener {
                showAuthor = !showAuthor
                val newText = if (showAuthor) {
                    buildSpannedString {
                        append("by ")
                        bold { append(author.name) }
                    }
                } else {
                    buildSpannedString { bold { append(product.packageName) } }
                }
                textSwitcher.setText(newText)
            }
        }
        name.text = product.name

        val installedItem = item.installedItem
        updateVersion(
            versionStr = installedItem?.version ?: product.version,
            canUpdate = product.canUpdate(installedItem)
        )

        size.text = item.sizeStr

        downloadsBlockDividier.isGone = item.downloads < 1
        downloadsBlock.isGone = item.downloads < 1
        downloads.text = item.downloads.toString()
        favouriteButton.isChecked = item.isFavourite
    }

    private fun updateVersion(versionStr: String, canUpdate: Boolean) {
        version.apply {
            text = versionStr
            if (canUpdate) {
                if (background == null) {
                    background = context.corneredBackground
                    setPadding(8.dp, 4.dp, 8.dp, 4.dp)
                    backgroundTintList =
                        context.getColorFromAttr(com.google.android.material.R.attr.colorTertiaryContainer)
                    setTextColor(context.getColorFromAttr(com.google.android.material.R.attr.colorOnTertiaryContainer))
                }
            } else {
                if (background != null) {
                    setPadding(0, 0, 0, 0)
                    setTextColor(
                        context.getColorFromAttr(android.R.attr.colorControlNormal),
                    )
                    background = null
                }
            }
        }
    }
}
