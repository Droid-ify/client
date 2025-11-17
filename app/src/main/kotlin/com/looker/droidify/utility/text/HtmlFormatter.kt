package com.looker.droidify.utility.text

import android.text.SpannableStringBuilder
import android.text.style.BulletSpan
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.View
import androidx.core.text.util.LinkifyCompat
import com.looker.droidify.data.model.Html

/**
 * Formats an HTML string into a styled [SpannableStringBuilder].
 *
 * Behaviors:
 * - Parses HTML with HtmlCompat.FROM_HTML_MODE_LEGACY
 * - Trims leading/trailing excessive newlines and collapses sequences of 3+ newlines into 2
 * - Linkifies web and email addresses
 * - Optionally replaces URLSpan with a [ClickableSpan] that invokes [onUrlClick]
 * - Replaces BulletSpan occurrences with a plain "• " bullet character
 */
fun Html.format(
    onUrlClick: ((String) -> Unit)? = null, // FIXME: Remove once legacy is removed
): SpannableStringBuilder {
    if (isPlainText) return SpannableStringBuilder(toString())

    val builder = run {
        val builder = SpannableStringBuilder(toSpanned())
        val last = builder.indexOfLast { it != '\n' }
        val first = builder.indexOfFirst { it != '\n' }
        if (last >= 0) {
            builder.delete(last + 1, builder.length)
        }
        if (first in 1 until last) {
            builder.delete(0, first - 1)
        }
        generateSequence(builder) {
            val index = it.indexOf("\n\n\n")
            if (index >= 0) it.delete(index, index + 1) else null
        }.last()
    }

    LinkifyCompat.addLinks(builder, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)

    if (onUrlClick != null) {
        val urlSpans = builder.getSpans(0, builder.length, URLSpan::class.java).orEmpty()
        for (span in urlSpans) {
            val start = builder.getSpanStart(span)
            val end = builder.getSpanEnd(span)
            val flags = builder.getSpanFlags(span)
            val url = span.url
            builder.removeSpan(span)
            builder.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onUrlClick.invoke(url)
                }
            }, start, end, flags)
        }
    }

    val bulletSpans = builder
        .getSpans(0, builder.length, BulletSpan::class.java)
        .orEmpty()
        .asSequence()
        .map { it to builder.getSpanStart(it) }
        .sortedByDescending { it.second }
    for ((span, start) in bulletSpans) {
        builder.removeSpan(span)
        builder.insert(start, "\u2022 ")
    }
    return builder
}

fun formatHtml(
    html: String,
    onUrlClick: ((String) -> Unit)?,
): SpannableStringBuilder = Html(html).format(onUrlClick)
