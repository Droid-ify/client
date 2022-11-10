package com.looker.core.model

import android.os.Parcel
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.looker.core.common.file.KParcelable
import com.looker.core.common.file.forEachKey

data class ProductItem(
	var repositoryId: Long, var packageName: String, var name: String, var summary: String,
	val icon: String, val metadataIcon: String, val version: String, var installedVersion: String,
	var compatible: Boolean, var canUpdate: Boolean, var matchRank: Int,
) {
	sealed class Section : KParcelable {
		object All : Section() {
			@Suppress("unused")
			@JvmField
			val CREATOR = KParcelable.creator { All }
		}

		data class Category(val name: String) : Section() {
			override fun writeToParcel(dest: Parcel, flags: Int) {
				dest.writeString(name)
			}

			companion object {
				@Suppress("unused")
				@JvmField
				val CREATOR = KParcelable.creator {
					val name = it.readString()!!
					Category(name)
				}
			}
		}

		data class Repository(val id: Long, val name: String) : Section() {
			override fun writeToParcel(dest: Parcel, flags: Int) {
				dest.writeLong(id)
				dest.writeString(name)
			}

			companion object {
				@Suppress("unused")
				@JvmField
				val CREATOR = KParcelable.creator {
					val id = it.readLong()
					val name = it.readString()!!
					Repository(id, name)
				}
			}
		}
	}

	fun serialize(generator: JsonGenerator) {
		generator.writeNumberField("serialVersion", 1)
		generator.writeNumberField("repositoryId", repositoryId)
		generator.writeStringField("packageName", packageName)
		generator.writeStringField("name", name)
		generator.writeStringField("summary", summary)
		generator.writeStringField("icon", icon)
		generator.writeStringField("metadataIcon", metadataIcon)
		generator.writeStringField("version", version)
		generator.writeStringField("installedVersion", installedVersion)
		generator.writeBooleanField("compatible", compatible)
		generator.writeBooleanField("canUpdate", canUpdate)
		generator.writeNumberField("matchRank", matchRank)
	}

	companion object {
		fun deserialize(parser: JsonParser): ProductItem {
			var repositoryId = 0L
			var packageName = ""
			var name = ""
			var summary = ""
			var icon = ""
			var metadataIcon = ""
			var version = ""
			var installedVersion = ""
			var compatible = false
			var canUpdate = false
			var matchRank = 0
			parser.forEachKey {
				when {
					it.number("repositoryId") -> repositoryId = valueAsLong
					it.string("packageName") -> packageName = valueAsString
					it.string("name") -> name = valueAsString
					it.string("summary") -> summary = valueAsString
					it.string("icon") -> icon = valueAsString
					it.string("metadataIcon") -> metadataIcon = valueAsString
					it.string("version") -> version = valueAsString
					it.string("installedVersion") -> installedVersion = valueAsString
					it.boolean("compatible") -> compatible = valueAsBoolean
					it.boolean("canUpdate") -> canUpdate = valueAsBoolean
					it.number("matchRank") -> matchRank = valueAsInt
					else -> skipChildren()
				}
			}
			return ProductItem(
				repositoryId, packageName, name, summary, icon, metadataIcon,
				version, installedVersion, compatible, canUpdate, matchRank
			)
		}
	}
}
