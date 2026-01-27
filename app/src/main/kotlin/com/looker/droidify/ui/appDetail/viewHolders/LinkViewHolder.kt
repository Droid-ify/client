package com.looker.droidify.ui.appDetail.viewHolders

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.imageview.ShapeableImageView
import com.looker.droidify.R
import com.looker.droidify.ui.appDetail.AppDetailAdapter
import com.looker.droidify.ui.appDetail.AppDetailItem
import com.looker.droidify.utility.common.extension.compatRequireViewById
import com.looker.droidify.utility.common.extension.copyLinkToClipboard
import com.looker.droidify.utility.extension.resources.TypefaceExtra
import kotlin.math.roundToInt

class LinkViewHolder(
    itemView: View,
    private val callbacks: AppDetailAdapter.Callbacks,
) : OverlappingViewHolder<AppDetailItem.LinkItem>(itemView), View.OnClickListener, View.OnLongClickListener {
    companion object {
        private val measurement: Measurement<Int> = Measurement()
    }

    private val icon: ShapeableImageView = itemView.compatRequireViewById(R.id.icon)
    private val text: TextView = itemView.compatRequireViewById(R.id.text)
    private val link: TextView = itemView.compatRequireViewById(R.id.link)

    init {
        text.typeface = TypefaceExtra.medium
        val margin = measurement.invalidate(itemView.resources) {
            @SuppressLint("SetTextI18n")
            text.text = "measure"
            link.visibility = View.GONE
            measurement.measure(itemView)
            ((itemView.measuredHeight - icon.measuredHeight) / 2f).roundToInt()
        }
        (icon.layoutParams as ViewGroup.MarginLayoutParams).apply {
            topMargin += margin
            bottomMargin += margin
        }

        itemView.setOnClickListener(this)
        itemView.setOnLongClickListener(this)
    }

    override fun onClick(v: View?) {
        if (v == itemView) {
            val uri = currentItem.uri
            if (uri == null || !callbacks.onUriClick(uri, false)) {
                copyDisplayLinkToClipboard()
            }
        }
    }

    override fun onLongClick(v: View?): Boolean {
        return if (v == itemView) {
            copyDisplayLinkToClipboard()
            true
        } else {
            false
        }
    }

    private fun copyDisplayLinkToClipboard() {
        currentItem.displayLink?.let { itemView.copyLinkToClipboard(it) }
    }

    override fun bindImpl(item: AppDetailItem.LinkItem) {
        itemView.isEnabled = item.uri != null
        icon.setImageResource(item.iconResId)
        text.text = item.getTitle(text.context)
        link.isVisible = item.uri != null
        link.text = item.displayLink
    }
}
