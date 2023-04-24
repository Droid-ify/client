package com.looker.core.data.utils

import androidx.core.os.LocaleListCompat
import androidx.core.os.LocaleListCompat.getEmptyLocaleList
import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalizationKtTest {

	@Test
	fun `Get correct localeList`() {
		assertEquals(
			LocaleListCompat.create(Locale.ENGLISH, Locale.US),
			localeListCompat("en,en-US")
		)
	}

	@Test
	fun `Return empty locale on none match`() {
		assertNull(emptyMap<String, String>().getBestLocale(localeListCompat("en-US,de-DE")))
		assertNull(getMap("en-US", "de-DE").getBestLocale(getEmptyLocaleList()))
	}

	@Test
	fun `Fallback to english`() {
		assertEquals(
			"en",
			getMap("de-AT", "de-DE", "en").getBestLocale(localeListCompat("fr-FR")),
		)
		assertEquals(
			"en-US",
			getMap("en", "en-US").getBestLocale(localeListCompat("zh-Hant-TW,zh-Hans-CN")),
		)
	}

	@Test
	fun `Use the first selected locale, en_US`() {
		assertEquals(
			"en-US",
			getMap("de-AT", "de-DE", "en-US").getBestLocale(localeListCompat("en-US,de-DE")),
		)
	}

	@Test
	fun `Use the first en translation`() {
		assertEquals(
			"en-US",
			getMap("de-AT", "de-DE", "en-US").getBestLocale(localeListCompat("en-SE,de-DE")),
		)
	}

	@Test
	fun `Use the first full match against a non-default locale`() {
		assertEquals(
			"de-AT",
			getMap(
				"de-AT",
				"de-DE",
				"en-GB",
				"en-US"
			).getBestLocale(localeListCompat("de-AT,de-DE")),
		)
		assertEquals(
			"de",
			getMap("de-AT", "de", "en-GB", "en-US").getBestLocale(localeListCompat("de-CH,en-US")),
		)
	}

	@Test
	fun `Stripped locale tag`() {
		assertEquals(
			"zh-TW",
			getMap(
				"en-US",
				"zh-CN",
				"zh-HK",
				"zh-TW"
			).getBestLocale(localeListCompat("zh-Hant-TW,zh-Hans-CN")),
		)
	}

	@Test
	fun `Google specified test`() {
		// https://developer.android.com/guide/topics/resources/multilingual-support#resource-resolution-examples
		assertEquals(
			"fr-FR",
			getMap("en-US", "de-DE", "es-ES", "fr-FR", "it-IT")
				.getBestLocale(localeListCompat("fr-CH")),
		)

		// https://developer.android.com/guide/topics/resources/multilingual-support#t-2d-choice
		assertEquals(
			"it-IT",
			getMap("en-US", "de-DE", "es-ES", "it-IT")
				.getBestLocale(localeListCompat("fr-CH,it-CH")),
		)
	}


	private fun getMap(vararg locales: String): Map<String, String> {
		return locales.associateWith { it }
	}
}