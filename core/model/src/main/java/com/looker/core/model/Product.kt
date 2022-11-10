package com.looker.core.model

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.looker.core.common.file.collectNotNull
import com.looker.core.common.file.collectNotNullStrings
import com.looker.core.common.file.forEachKey
import com.looker.core.common.file.writeArray
import com.looker.core.common.file.writeDictionary
import com.looker.core.common.nullIfEmpty

data class Product(
	var repositoryId: Long,
	val packageName: String,
	val name: String,
	val summary: String,
	var description: String,
	val whatsNew: String,
	val icon: String,
	val metadataIcon: String,
	val author: Author,
	val source: String,
	val changelog: String,
	val web: String,
	val tracker: String,
	val added: Long,
	val updated: Long,
	val suggestedVersionCode: Long,
	val categories: List<String>,
	val antiFeatures: List<String>,
	val licenses: List<String>,
	val donates: List<Donate>,
	val screenshots: List<Screenshot>,
	val releases: List<Release>,
) {
	data class Author(val name: String, val email: String, val web: String)

	sealed class Donate {
		data class Regular(val url: String) : Donate()
		data class Bitcoin(val address: String) : Donate()
		data class Litecoin(val address: String) : Donate()
		data class Flattr(val id: String) : Donate()
		data class Liberapay(val id: String) : Donate()
		data class OpenCollective(val id: String) : Donate()
	}

	class Screenshot(val locale: String, val type: Type, val path: String) {
		enum class Type(val jsonName: String) {
			PHONE("phone"),
			SMALL_TABLET("smallTablet"),
			LARGE_TABLET("largeTablet")
		}

		val identifier: String
			get() = "$locale.${type.name}.$path"
	}

	// Same releases with different signatures
	val selectedReleases: List<Release>
		get() = releases.filter { it.selected }

	val displayRelease: Release?
		get() = selectedReleases.firstOrNull() ?: releases.firstOrNull()

	val version: String
		get() = displayRelease?.version.orEmpty()

	val versionCode: Long
		get() = selectedReleases.firstOrNull()?.versionCode ?: 0L

	val compatible: Boolean
		get() = selectedReleases.firstOrNull()?.incompatibilities?.isEmpty() == true

	val signatures: List<String>
		get() = selectedReleases.mapNotNull { it.signature.nullIfEmpty() }.distinct().toList()

	fun item(): ProductItem {
		return ProductItem(
			repositoryId,
			packageName,
			name,
			summary,
			icon,
			metadataIcon,
			version,
			"",
			compatible,
			false,
			0
		)
	}

	fun canUpdate(installedItem: InstalledItem?): Boolean {
		return installedItem != null && compatible && versionCode > installedItem.versionCode &&
				installedItem.signature in signatures
	}

	fun serialize(generator: JsonGenerator) {
		generator.writeNumberField("repositoryId", repositoryId)
		generator.writeNumberField("serialVersion", 1)
		generator.writeStringField("packageName", packageName)
		generator.writeStringField("name", name)
		generator.writeStringField("summary", summary)
		generator.writeStringField("description", description)
		generator.writeStringField("whatsNew", whatsNew)
		generator.writeStringField("icon", icon)
		generator.writeStringField("metadataIcon", metadataIcon)
		generator.writeStringField("authorName", author.name)
		generator.writeStringField("authorEmail", author.email)
		generator.writeStringField("authorWeb", author.web)
		generator.writeStringField("source", source)
		generator.writeStringField("changelog", changelog)
		generator.writeStringField("web", web)
		generator.writeStringField("tracker", tracker)
		generator.writeNumberField("added", added)
		generator.writeNumberField("updated", updated)
		generator.writeNumberField("suggestedVersionCode", suggestedVersionCode)
		generator.writeArray("categories") { categories.forEach(::writeString) }
		generator.writeArray("antiFeatures") { antiFeatures.forEach(::writeString) }
		generator.writeArray("licenses") { licenses.forEach(::writeString) }
		generator.writeArray("donates") {
			donates.forEach {
				writeDictionary {
					when (it) {
						is Donate.Regular -> {
							writeStringField("type", "")
							writeStringField("url", it.url)
						}
						is Donate.Bitcoin -> {
							writeStringField("type", "bitcoin")
							writeStringField("address", it.address)
						}
						is Donate.Litecoin -> {
							writeStringField("type", "litecoin")
							writeStringField("address", it.address)
						}
						is Donate.Flattr -> {
							writeStringField("type", "flattr")
							writeStringField("id", it.id)
						}
						is Donate.Liberapay -> {
							writeStringField("type", "liberapay")
							writeStringField("id", it.id)
						}
						is Donate.OpenCollective -> {
							writeStringField("type", "openCollective")
							writeStringField("id", it.id)
						}
					}::class
				}
			}
		}
		generator.writeArray("screenshots") {
			screenshots.forEach {
				writeDictionary {
					writeStringField("locale", it.locale)
					writeStringField("type", it.type.jsonName)
					writeStringField("path", it.path)
				}
			}
		}
		generator.writeArray("releases") { releases.forEach { writeDictionary { it.serialize(this) } } }
	}

	companion object {
		fun <T> findSuggested(
			products: List<T>,
			installedItem: InstalledItem?,
			extract: (T) -> Product,
		): T? {
			return products.maxWithOrNull(compareBy({
				extract(it).compatible &&
						(installedItem == null || installedItem.signature in extract(it).signatures)
			}, { extract(it).versionCode }))
		}

		fun deserialize(parser: JsonParser): Product {
			var repositoryId = 0L
			var packageName = ""
			var name = ""
			var summary = ""
			var description = ""
			var whatsNew = ""
			var icon = ""
			var metadataIcon = ""
			var authorName = ""
			var authorEmail = ""
			var authorWeb = ""
			var source = ""
			var changelog = ""
			var web = ""
			var tracker = ""
			var added = 0L
			var updated = 0L
			var suggestedVersionCode = 0L
			var categories = emptyList<String>()
			var antiFeatures = emptyList<String>()
			var licenses = emptyList<String>()
			var donates = emptyList<Donate>()
			var screenshots = emptyList<Screenshot>()
			var releases = emptyList<Release>()
			parser.forEachKey { it ->
				when {
					it.string("repositoryId") -> repositoryId = valueAsLong
					it.string("packageName") -> packageName = valueAsString
					it.string("name") -> name = valueAsString
					it.string("summary") -> summary = valueAsString
					it.string("description") -> description = valueAsString
					it.string("whatsNew") -> whatsNew = valueAsString
					it.string("icon") -> icon = valueAsString
					it.string("metadataIcon") -> metadataIcon = valueAsString
					it.string("authorName") -> authorName = valueAsString
					it.string("authorEmail") -> authorEmail = valueAsString
					it.string("authorWeb") -> authorWeb = valueAsString
					it.string("source") -> source = valueAsString
					it.string("changelog") -> changelog = valueAsString
					it.string("web") -> web = valueAsString
					it.string("tracker") -> tracker = valueAsString
					it.number("added") -> added = valueAsLong
					it.number("updated") -> updated = valueAsLong
					it.number("suggestedVersionCode") -> suggestedVersionCode = valueAsLong
					it.array("categories") -> categories = collectNotNullStrings()
					it.array("antiFeatures") -> antiFeatures = collectNotNullStrings()
					it.array("licenses") -> licenses = collectNotNullStrings()
					it.array("donates") -> donates = collectNotNull(JsonToken.START_OBJECT) {
						var type = ""
						var url = ""
						var address = ""
						var id = ""
						forEachKey {
							when {
								it.string("type") -> type = valueAsString
								it.string("url") -> url = valueAsString
								it.string("address") -> address = valueAsString
								it.string("id") -> id = valueAsString
								else -> skipChildren()
							}
						}
						when (type) {
							"" -> Donate.Regular(url)
							"bitcoin" -> Donate.Bitcoin(address)
							"litecoin" -> Donate.Litecoin(address)
							"flattr" -> Donate.Flattr(id)
							"liberapay" -> Donate.Liberapay(id)
							"openCollective" -> Donate.OpenCollective(id)
							else -> null
						}
					}
					it.array("screenshots") -> screenshots =
						collectNotNull(JsonToken.START_OBJECT) {
							var locale = ""
							var type = ""
							var path = ""
							forEachKey {
								when {
									it.string("locale") -> locale = valueAsString
									it.string("type") -> type = valueAsString
									it.string("path") -> path = valueAsString
									else -> skipChildren()
								}
							}
							Screenshot.Type.values().find { it.jsonName == type }
								?.let { Screenshot(locale, it, path) }
						}
					it.array("releases") -> releases =
						collectNotNull(JsonToken.START_OBJECT, Release.Companion::deserialize)
					else -> skipChildren()
				}
			}
			return Product(
				repositoryId,
				packageName,
				name,
				summary,
				description,
				whatsNew,
				icon,
				metadataIcon,
				Author(authorName, authorEmail, authorWeb),
				source,
				changelog,
				web,
				tracker,
				added,
				updated,
				suggestedVersionCode,
				categories,
				antiFeatures,
				licenses,
				donates,
				screenshots,
				releases
			)
		}
	}
}
