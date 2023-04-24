package com.looker.core.data.utils

import androidx.core.os.LocaleListCompat
import androidx.core.os.LocaleListCompat.getEmptyLocaleList
import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull

// https://developer.android.com/guide/topics/resources/multilingual-support#resource-resolution-examples
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
		assertNull(emptyMap<String, String>().localizedValue(localeListCompat("en-US,de-DE")))
		assertNull(getMap("en-US", "de-DE").localizedValue(getEmptyLocaleList()))
	}

	@Test
	fun `Fallback to english`() {
		assertEquals(
			"en",
			getMap("de-AT", "de-DE", "en").localizedValue(localeListCompat("fr-FR")),
		)
		assertEquals(
			"en-US",
			getMap("en", "en-US").localizedValue(localeListCompat("zh-Hant-TW,zh-Hans-CN")),
		)
	}

	@Test
	fun `Use the first selected locale, en_US`() {
		assertEquals(
			"en-US",
			getMap("de-AT", "de-DE", "en-US").localizedValue(localeListCompat("en-US,de-DE")),
		)
	}

	@Test
	fun `Use the first en translation`() {
		assertEquals(
			"en-US",
			getMap("de-AT", "de-DE", "en-US").localizedValue(localeListCompat("en-SE,de-DE")),
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
			).localizedValue(localeListCompat("de-AT,de-DE")),
		)
		assertEquals(
			"de",
			getMap("de-AT", "de", "en-GB", "en-US").localizedValue(localeListCompat("de-CH,en-US")),
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
			).localizedValue(localeListCompat("zh-Hant-TW,zh-Hans-CN")),
		)
	}

	@Test
	fun `Google specified test`() {
		assertEquals(
			"fr-FR",
			getMap("en-US", "de-DE", "es-ES", "fr-FR", "it-IT")
				.localizedValue(localeListCompat("fr-CH")),
		)

		assertEquals(
			"it-IT",
			getMap("en-US", "de-DE", "es-ES", "it-IT")
				.localizedValue(localeListCompat("fr-CH,it-CH")),
		)
	}

	@Test
	fun `Check null for suitable locale from list`() {
		assertNull(localeListCompat("en-US").suitableLocale(keys("de-DE", "es-ES")))
		assertNull(localeListCompat("en-US").suitableLocale(keys()))
		assertNull(getEmptyLocaleList().suitableLocale(keys("de-DE", "es-ES")))
	}

	@Test
	fun `Find suitable locale from wrong list`() {
		assertNull(localeListCompat("en-US").suitableLocale(keys("de-DE", "es-ES")))
	}

	@Test
	fun `Find suitable locale from list without modification`() {
		assertEquals(
			"en-US",
			localeListCompat("en-US").suitableLocale(keys("en", "en-US", "en-UK"))
		)
	}

	@Test
	fun `Find suitable locale from list only with language`() {
		assertEquals(
			"en",
			localeListCompat("en-US").suitableLocale(keys("de-DE", "fr-FR", "en-UK", "en"))
		)
	}

	@Test
	fun `Find stripped locale from the list`() {
		assertEquals(
			"zh-TW",
			localeListCompat("zh-Hant-TW").suitableLocale(
				keys(
					"en",
					"de-DE",
					"fr-FR",
					"zh-TW",
					"zh"
				)
			)
		)
	}

	@Test
	fun `Check null for suitable locale`() {
		val locale: Locale? = null
		assertNull(locale.suitableTag(keys("en-US", "de-DE", "es-ES", "it-IT")))
		assertNull(Locale.ENGLISH.suitableTag(keys()))
	}

	@Test
	fun `Find suitable locale from wrong keys`() {
		assertNull(Locale.ENGLISH.suitableTag(keys("de-DE", "es-ES")))
	}

	@Test
	fun `Get suitable locale without modification`() {
		assertEquals("en-US", Locale("en", "US").suitableTag(keys("en", "en-US", "en-UK")))
	}

	@Test
	fun `Get suitable locale with only language`() {
		assertEquals("en", Locale("en", "US").suitableTag(keys("en", "de-DE", "fr-FR")))
	}

	@Test
	fun `Get suitable locale with stripped parts`() {
		assertEquals(
			"zh-TW",
			localeListCompat("zh-Hant-TW")[0].suitableTag(
				keys(
					"en",
					"de-DE",
					"fr-FR",
					"zh-TW",
					"zh"
				)
			)
		)
	}

	private fun keys(vararg tag: String): Set<String> = tag.toSet()

	private fun getMap(vararg locales: String): Map<String, String> = locales.associateWith { it }
}