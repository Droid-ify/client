package com.looker.droidify.utility.text

import android.graphics.Typeface
import android.text.Spannable
import android.text.style.CharacterStyle
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.text.util.Linkify
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.core.text.HtmlCompat
import androidx.core.text.util.LinkifyCompat
import com.looker.droidify.data.model.Html

/**
 * Add formatter which supports:
 *
 * b, big, blockquote, br, cite, em, i, li, ol, small, strike, strong, sub, sup, tt, u, ul
 * optionally: a + href
 *
 * */
@Suppress("DEPRECATION")
fun Html.toAnnotatedString(
    style: SpanStyle = SpanStyle(),
    linkStyle: SpanStyle = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline),
    onUrlClick: (url: String) -> Unit = {},
    onEmailClick: (email: String) -> Unit = {},
): AnnotatedString {
    val spanned = HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_COMPACT) as Spannable
    LinkifyCompat.addLinks(spanned, Linkify.ALL)

    val spans = spanned.getSpans(0, spanned.length, CharacterStyle::class.java)

    val builder = AnnotatedString.Builder(spanned.toString())

    return with(builder) {
        for (span in spans) {
            val start = spanned.getSpanStart(span)
            if (start == -1) continue
            val end = spanned.getSpanEnd(span)
            if (end == -1) continue
            if (span !is URLSpan) {
                addStyle(span.toSpanStyle(style), start, end)
            } else {
                val url = span.url
                if (url.startsWith("mailto:")) {
                    addLink(
                        start = start,
                        end = end,
                        clickable = LinkAnnotation.Clickable(
                            styles = TextLinkStyles(style = linkStyle),
                            tag = url,
                            linkInteractionListener = { onEmailClick(url.removePrefix("mailto:")) }
                        )
                    )
                } else {
                    addLink(
                        start = start,
                        end = end,
                        url = LinkAnnotation.Url(
                            styles = TextLinkStyles(style = linkStyle),
                            url = url,
                            linkInteractionListener = { onUrlClick(url) }
                        )
                    )
                }
            }

        }
        toAnnotatedString()
    }
}

private fun <T : CharacterStyle> T.toSpanStyle(base: SpanStyle) = when (this) {
    is UnderlineSpan -> base.copy(textDecoration = TextDecoration.Underline)
    is StrikethroughSpan -> base.copy(textDecoration = TextDecoration.LineThrough)
    is SuperscriptSpan -> base.copy(fontSize = 0.8.em, baselineShift = BaselineShift.Superscript)
    is SubscriptSpan -> base.copy(fontSize = 0.8.em, baselineShift = BaselineShift.Subscript)
    is RelativeSizeSpan -> base.copy(fontSize = sizeChange.em)

    is StyleSpan -> when (style) {
        Typeface.BOLD -> base.copy(fontWeight = FontWeight.Bold)
        Typeface.ITALIC -> base.copy(fontStyle = FontStyle.Italic)
        else -> base
    }

    is TypefaceSpan -> when (family) {
        "monospace" -> base.copy(fontFamily = FontFamily.Monospace)
        else -> base
    }

    else -> base
}
