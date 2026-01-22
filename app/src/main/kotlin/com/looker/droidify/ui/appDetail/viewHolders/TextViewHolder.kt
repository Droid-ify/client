package com.looker.droidify.ui.appDetail.viewHolders

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.looker.droidify.ui.appDetail.AppDetailAdapter
import com.looker.droidify.ui.appDetail.AppDetailItem
import com.looker.droidify.utility.common.extension.dp
import com.looker.droidify.utility.common.extension.setTextSizeScaled
import com.looker.droidify.utility.common.extension.submitText
import com.looker.droidify.utility.text.ClickableUrlSpan

class TextViewHolder(
    private val textView: AppCompatTextView,
    private val callbacks: AppDetailAdapter.Callbacks,
) : BaseViewHolder<AppDetailItem.TextItem>(textView), ClickableUrlSpan.ClickHandler {

    constructor(
        context: Context,
        callbacks: AppDetailAdapter.Callbacks,
    ): this(
        textView = createTextElementLayout(context = context),
        callbacks = callbacks,
    )

    override fun bindImpl(item: AppDetailItem.TextItem) {
        textView.submitText(updateUrlHandlers(item.text))
    }

    private fun updateUrlHandlers(c: CharSequence): CharSequence {
        if (c is SpannableStringBuilder) {
            val spans = c.getSpans(
                /* queryStart = */ 0,
                /* queryEnd = */ c.length,
                /* kind = */ ClickableUrlSpan::class.java,
            )
            spans.forEach {
                it.clickHandler = this
            }
        }

        return c
    }

    override fun onUrlClick(url: String) {
        val uri = try {
            url.toUri()
        } catch (_: Exception) {
            null
        }
        if (uri != null) {
            callbacks.onUriClick(uri, true)
        }
    }
}

fun createTextElementLayout(context: Context): AppCompatTextView {
    return AppCompatTextView(context).apply {
        setTextIsSelectable(true)
        setTextSizeScaled(15)
        isFocusable = false
        16.dp.let { setPadding(it, it, it, it) }
        movementMethod = LinkMovementMethod()
        layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT,
        )
    }
}
