package com.looker.core_data.data

import android.os.Build
import com.looker.core_data.IndexParser
import com.looker.core_data.IndexParser.Companion.validateIcon
import com.looker.core_data.ParserCallback
import com.looker.core_database.model.Apk
import com.looker.core_database.model.App
import kotlinx.coroutines.runBlocking
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class OldIndexParser : IndexParser<DefaultHandler> {
	override suspend fun parseIndex(
		repoId: Long,
		inputStream: InputStream,
		parserCallback: ParserCallback
	): DefaultHandler = LegacyIndexHandler(repoId, parserCallback)
}

internal class LegacyIndexHandler(
	private val repoId: Long,
	private val callback: ParserCallback
) : DefaultHandler() {

	companion object {
		private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
			.apply { timeZone = TimeZone.getTimeZone("UTC") }

		private fun String.parseDate(): Long {
			return try {
				dateFormat.parse(this)?.time ?: 0L
			} catch (e: Exception) {
				0L
			}
		}
	}

	private class RepoBuilder {
		var address = ""
		val mirrors = mutableListOf<String>()
		var name = ""
		var description = ""
		var certificate = ""
		var version = -1
		var timestamp = 0L
	}

	private class AppBuilder(val repoId: Long, val packageName: String) {
		var name = ""
		var summary = ""
		var description = ""
		var icon = ""
		var authorName = ""
		var authorEmail = ""
		var source = ""
		var changelog = ""
		var web = ""
		var tracker = ""
		var added = 0L
		var updated = 0L
		var suggestedVersionCode = 0L
		val categories = linkedSetOf<String>()
		val antiFeatures = linkedSetOf<String>()
		val licenses = mutableListOf<String>()
		var donate = ""
		var bitcoin = ""
		var litecoin = ""
		var liberapayID = ""
		var flattrID = ""
		var openCollective = ""
		val apks = mutableListOf<Apk>()

		fun build(): App = App(
			packageName = packageName,
			nameFallback = name,
			descriptionFallback = description,
			summaryFallback = summary,
			iconFallback = icon,
			authorName = authorName,
			authorEmail = authorEmail,
			authorWebsite = "",
			license = licenses[0],
			website = web,
			sourceCode = source,
			changelog = changelog,
			issueTracker = tracker,
			helpTranslate = "",
			added = added,
			lastUpdated = updated,
			suggestedVersionName = "",
			suggestedVersionCode = suggestedVersionCode,
			categories = categories.toList(),
			antiFeatures = antiFeatures.toList(),
			regularDonate = donate,
			bitcoinId = bitcoin,
			liteCoinAddress = litecoin,
			flattrId = flattrID,
			liberaPay = liberapayID,
			openCollective = openCollective,
			localized = emptyMap(),
			repoId = repoId,
			apks = apks
		)
	}

	private class ApkBuilder {
		var versionName = ""
		var versionCode = 0L
		var added = 0L
		var size = 0L
		var minSdkVersion = 0
		var targetSdkVersion = 0
		var maxSdkVersion = 0
		var source = ""
		var apkName = ""
		var hash = ""
		var hashType = ""
		var signature = ""
		val permissions = linkedSetOf<String>()
		val features = linkedSetOf<String>()
		val platforms = linkedSetOf<String>()

		fun build(): Apk {
			val hashType = if (hash.isNotEmpty() && hashType.isEmpty()) "sha256" else hashType

			return Apk(
				apkName = apkName,
				size = size,
				added = added,
				versionCode = versionCode,
				versionName = versionName,
				hash = hash,
				hashType = hashType,
				signature = signature,
				signer = "",
				srcName = source,
				minSdk = minSdkVersion,
				maxSdk = maxSdkVersion,
				targetSdk = targetSdkVersion,
				features = features.toList(),
				platforms = platforms.toList()
			)
		}
	}

	private val contentBuilder = StringBuilder()

	private var repoBuilder: RepoBuilder? = RepoBuilder()
	private var appBuilder: AppBuilder? = null
	private var apkBuilder: ApkBuilder? = null

	private fun Attributes.get(localName: String): String = getValue("", localName).orEmpty()
	private fun String.cleanWhiteSpace(): String = replace("\\s".toRegex(), " ")

	override fun startElement(
		uri: String,
		localName: String,
		qName: String,
		attributes: Attributes,
	) {
		super.startElement(uri, localName, qName, attributes)

		val repoBuilder = repoBuilder
		val appBuilder = appBuilder
		val apkBuilder = apkBuilder
		contentBuilder.setLength(0)

		when {
			localName == "repo" -> {
				if (repoBuilder != null) {
					repoBuilder.address = attributes.get("url").cleanWhiteSpace()
					repoBuilder.name = attributes.get("name").cleanWhiteSpace()
					repoBuilder.description = attributes.get("description").cleanWhiteSpace()
					repoBuilder.certificate = attributes.get("pubkey")
					repoBuilder.version = attributes.get("version").toIntOrNull() ?: 0
					repoBuilder.timestamp =
						(attributes.get("timestamp").toLongOrNull() ?: 0L) * 1000L
				}
			}
			localName == "application" && appBuilder == null -> {
				this.appBuilder = AppBuilder(repoId, attributes.get("id"))
			}
			localName == "package" && appBuilder != null && apkBuilder == null -> {
				this.apkBuilder = ApkBuilder()
			}
			localName == "hash" && apkBuilder != null -> {
				apkBuilder.hashType = attributes.get("type")
			}
			(localName == "uses-permission" || localName.startsWith("uses-permission-")) && apkBuilder != null -> {
				val minSdkVersion = if (localName != "uses-permission") {
					"uses-permission-sdk-(\\d+)".toRegex().matchEntire(localName)
						?.destructured?.let { (version) -> version.toIntOrNull() }
				} else {
					null
				} ?: 0
				val maxSdkVersion = attributes.get("maxSdkVersion").toIntOrNull() ?: Int.MAX_VALUE
				if (Build.VERSION.SDK_INT in minSdkVersion..maxSdkVersion) {
					apkBuilder.permissions.add(attributes.get("name"))
				} else {
					apkBuilder.permissions.remove(attributes.get("name"))
				}
			}
		}
	}

	override fun endElement(uri: String, localName: String, qName: String) {
		super.endElement(uri, localName, qName)

		val repoBuilder = repoBuilder
		val appBuilder = appBuilder
		val apkBuilder = apkBuilder
		val content = contentBuilder.toString()

		when {
			localName == "repo" -> {
				if (repoBuilder != null) {
					val mirrors = (listOf(repoBuilder.address) + repoBuilder.mirrors)
						.filter { it.isNotEmpty() }.distinct()
					// TODO: Fix 
					runBlocking {
						callback.onRepo(
							mirrors,
							repoBuilder.name,
							repoBuilder.description,
							repoBuilder.version,
							repoBuilder.timestamp
						)
					}
					this.repoBuilder = null
				}
			}
			localName == "application" && appBuilder != null -> {
				val app = appBuilder.build()
				this.appBuilder = null
				// TODO: Fix
				runBlocking { callback.onApp(app) }
			}
			localName == "package" && appBuilder != null && apkBuilder != null -> {
				appBuilder.apks.add(apkBuilder.build())
				this.apkBuilder = null
			}
			repoBuilder != null -> {
				when (localName) {
					"description" -> repoBuilder.description = content.cleanWhiteSpace()
					"mirror" -> repoBuilder.mirrors += content
				}
			}
			appBuilder != null && apkBuilder != null -> {
				when (localName) {
					"version" -> apkBuilder.versionName = content
					"versioncode" -> apkBuilder.versionCode = content.toLongOrNull() ?: 0L
					"added" -> apkBuilder.added = content.parseDate()
					"size" -> apkBuilder.size = content.toLongOrNull() ?: 0
					"sdkver" -> apkBuilder.minSdkVersion = content.toIntOrNull() ?: 0
					"targetSdkVersion" -> apkBuilder.targetSdkVersion =
						content.toIntOrNull() ?: 0
					"maxsdkver" -> apkBuilder.maxSdkVersion = content.toIntOrNull() ?: 0
					"srcname" -> apkBuilder.source = content
					"apkname" -> apkBuilder.apkName = content
					"hash" -> apkBuilder.hash = content
					"sig" -> apkBuilder.signature = content
					"permissions" -> apkBuilder.permissions += content.split(',')
						.filter { it.isNotEmpty() }
					"features" -> apkBuilder.features += content.split(',')
						.filter { it.isNotEmpty() }
					"nativecode" -> apkBuilder.platforms += content.split(',')
						.filter { it.isNotEmpty() }
				}
			}
			appBuilder != null -> {
				when (localName) {
					"name" -> appBuilder.name = content
					"summary" -> appBuilder.summary = content
					"description" -> appBuilder.description = "<p>$content</p>"
					"desc" -> appBuilder.description = content.replace("\n", "<br/>")
					"icon" -> appBuilder.icon = validateIcon(content)
					"author" -> appBuilder.authorName = content
					"email" -> appBuilder.authorEmail = content
					"source" -> appBuilder.source = content
					"changelog" -> appBuilder.changelog = content
					"web" -> appBuilder.web = content
					"tracker" -> appBuilder.tracker = content
					"added" -> appBuilder.added = content.parseDate()
					"lastupdated" -> appBuilder.updated = content.parseDate()
					"marketvercode" -> appBuilder.suggestedVersionCode =
						content.toLongOrNull() ?: 0L
					"categories" -> appBuilder.categories += content.split(',')
						.filter { it.isNotEmpty() }
					"antifeatures" -> appBuilder.antiFeatures += content.split(',')
						.filter { it.isNotEmpty() }
					"license" -> appBuilder.licenses += content.split(',')
						.filter { it.isNotEmpty() }
					"donate" -> appBuilder.donate = content
					"bitcoin" -> appBuilder.bitcoin = content
					"litecoin" -> appBuilder.litecoin = content
					"flattr" -> appBuilder.flattrID = content
					"liberapay" -> appBuilder.liberapayID = content
					"openCollective" -> appBuilder.openCollective = content
				}
			}
		}
	}
}