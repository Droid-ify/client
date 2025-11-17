package com.looker.droidify.sync.v2.model

import androidx.core.os.LocaleListCompat

typealias LocalizedString = Map<String, String>
typealias NullableLocalizedString = Map<String, String?>
typealias LocalizedIcon = Map<String, FileV2>
typealias LocalizedList = Map<String, List<String>>
typealias LocalizedFiles = Map<String, List<FileV2>>

typealias DefaultName = String
typealias Tag = String

typealias AntiFeatureReason = LocalizedString

fun <T> Map<String, T>?.localizedValue(locale: String): T? {
    if (isNullOrEmpty()) return null
    val localeList = LocaleListCompat.forLanguageTags(locale)
    val match = localeList.getFirstMatch(keys.toTypedArray()) ?: return null
    return get(match.toLanguageTag()) ?: run {
        val langCountryTag = "${match.language}-${match.country}"
        getOrStartsWith(langCountryTag) ?: run {
            val langTag = match.language
            getOrStartsWith(langTag) ?: get("en-US") ?: get("en") ?: values.first()
        }
    }
}

private fun <T> Map<String, T>.getOrStartsWith(s: String): T? = get(s) ?: run {
    entries.forEach { (key, value) ->
        if (key.startsWith(s)) return value
    }
    return null
}
