package com.looker.droidify.data.model

import android.text.Spanned
import androidx.compose.runtime.Immutable
import androidx.core.text.HtmlCompat

private const val TagSelfClosing = "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)/\\>"

private const val TagStart = "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)\\>"
private const val TagEnd = "\\</\\w+\\>"

private const val HtmlEntity = "&[a-zA-Z][a-zA-Z0-9]+;"

private val htmlRegex = Regex(
    pattern = "(${TagStart}.*$TagEnd)|($TagSelfClosing)|($HtmlEntity)",
    option = RegexOption.DOT_MATCHES_ALL
)

@JvmInline
@Immutable
value class Html(val raw: String) : CharSequence {

    val isPlainText: Boolean get() = !raw.contains(htmlRegex)

    fun toSpanned(): Spanned = HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY)

    override val length: Int get() = raw.length

    override fun get(index: Int): Char = raw[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = raw.subSequence(startIndex, endIndex)

    override fun toString(): String = raw

}
