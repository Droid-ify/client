package com.looker.droidify.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.core.text.HtmlCompat

@JvmInline
@Immutable
value class Html(val raw: String) : CharSequence {
    override val length: Int
        get() = raw.length

    override fun get(index: Int): Char = raw[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        raw.subSequence(startIndex, endIndex)

    override fun toString(): String = raw

    val isPlainText: Boolean
        get() = !raw.contains(htmlRegex)

    val isValid: Boolean
        get() = raw.contains(htmlRegex)

    private companion object {
        const val TagStart = "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)\\>"
        const val TagEnd = "\\</\\w+\\>"

        const val TagSelfClosing = "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)/\\>"

        const val HtmlEntity = "&[a-zA-Z][a-zA-Z0-9]+;"

        val htmlRegex = Regex(
            pattern = "(${TagStart}.*$TagEnd)|($TagSelfClosing)|($HtmlEntity)",
            option = RegexOption.DOT_MATCHES_ALL
        )
    }
}

fun Html.toCompat(flags: Int = HtmlCompat.FROM_HTML_MODE_LEGACY) = HtmlCompat.fromHtml(raw, flags)

/**
 * Add formatter which supports:
 *
 * b, big, blockquote, br, cite, em, i, li, ol, small, strike, strong, sub, sup, tt, u, ul
 * optionally: a + href
 *
 * */
fun Html.toAnnotatedString(
    style: SpanStyle,
    linkStyle: SpanStyle,
    onUrlClick: (url: String) -> Unit = {},
    onEmailClick: (email: String) -> Unit = {},
): AnnotatedString {
    return buildAnnotatedString {
        append(raw)
    }
}
