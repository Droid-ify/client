package com.looker.core.data.utils

import androidx.core.os.LocaleListCompat
import com.looker.core.common.stripBetween

fun localeListCompat(tag: String): LocaleListCompat = LocaleListCompat.forLanguageTags(tag)

@OptIn(ExperimentalStdlibApi::class)
fun <T> Map<String, T>?.getBestLocale(localeList: LocaleListCompat): T? {
	if (isNullOrEmpty()) return null
	if (localeList.isEmpty) return null
	var bestLocale: String? = null
	for (i in 0..<localeList.size()) {
		val currentLocale = localeList[i] ?: continue
		val tag = currentLocale.toLanguageTag()
		val soloTag = currentLocale.language
		val strippedTag = tag.stripBetween("-")

		bestLocale = if (containsKey(tag)) tag
		else if (containsKey(strippedTag)) strippedTag
		else if (containsKey(soloTag)) soloTag
		// try children of the language
		else keys.find { it.startsWith(soloTag) } ?: continue
		if (bestLocale != null) break
	}
	return get(bestLocale) ?: get("en_US") ?: get("en-US") ?: get("en") ?: values.firstOrNull()
}