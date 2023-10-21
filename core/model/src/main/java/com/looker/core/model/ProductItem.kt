package com.looker.core.model

import android.os.Parcel
import com.looker.core.model.util.KParcelable

data class ProductItem(
    var repositoryId: Long,
    var packageName: String,
    var name: String,
    var summary: String,
    val icon: String,
    val metadataIcon: String,
    val version: String,
    var installedVersion: String,
    var compatible: Boolean,
    var canUpdate: Boolean,
    var matchRank: Int
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
}
