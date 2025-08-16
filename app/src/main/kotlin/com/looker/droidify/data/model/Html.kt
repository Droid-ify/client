package com.looker.droidify.data.model

import androidx.compose.runtime.Immutable
import androidx.core.text.HtmlCompat

@JvmInline
@Immutable
value class Html(val raw: String) : CharSequence {
    override val length: Int
        get() = raw.length

    override fun get(index: Int): Char = raw[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        raw.subSequence(startIndex, endIndex)

}

fun Html.toCompat(flags: Int = HtmlCompat.FROM_HTML_MODE_LEGACY) = HtmlCompat.fromHtml(raw, flags)
