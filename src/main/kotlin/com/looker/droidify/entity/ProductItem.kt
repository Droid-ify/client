package com.looker.droidify.entity

import android.os.Parcel
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.looker.droidify.R
import com.looker.droidify.utility.KParcelable
import com.looker.droidify.utility.extension.json.forEachKey

data class ProductItem(
    val repositoryId: Long, val packageName: String, val name: String, val summary: String,
    val icon: String, val metadataIcon: String, val version: String, val installedVersion: String,
    val compatible: Boolean, val canUpdate: Boolean, val matchRank: Int
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

    enum class Order(val titleResId: Int) {
        NAME(R.string.name),
        DATE_ADDED(R.string.date_added),
        LAST_UPDATE(R.string.last_update)
    }

    fun serialize(generator: JsonGenerator) {
        generator.writeNumberField("serialVersion", 1)
        generator.writeStringField("icon", icon)
        generator.writeStringField("metadataIcon", metadataIcon)
        generator.writeStringField("version", version)
    }

    companion object {
        fun deserialize(
            repositoryId: Long, packageName: String, name: String, summary: String,
            installedVersion: String, compatible: Boolean, canUpdate: Boolean, matchRank: Int,
            parser: JsonParser
        ): ProductItem {
            var icon = ""
            var metadataIcon = ""
            var version = ""
            parser.forEachKey {
                when {
                    it.string("icon") -> icon = valueAsString
                    it.string("metadataIcon") -> metadataIcon = valueAsString
                    it.string("version") -> version = valueAsString
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
