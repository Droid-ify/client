package com.looker.droidify.data.local.model

import androidx.annotation.IntDef
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import com.looker.droidify.data.model.Donation
import com.looker.droidify.sync.v2.model.MetadataV2

@Entity(
    tableName = "donate",
    primaryKeys = ["type", "appId"],
    indices = [Index("appId")],
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            childColumns = ["appId"],
            parentColumns = ["id"],
            onDelete = CASCADE,
        ),
    ],
)
data class DonateEntity(
    @param:DonationType
    val type: Int,
    val value: String,
    val appId: Int,
)

fun MetadataV2.donateEntity(appId: Int): List<DonateEntity>? {
    return buildList {
        if (bitcoin != null) {
            add(DonateEntity(BITCOIN_ADD, bitcoin, appId))
        }
        if (litecoin != null) {
            add(DonateEntity(LITECOIN_ADD, litecoin, appId))
        }
        if (liberapay != null) {
            add(DonateEntity(LIBERAPAY_ID, liberapay, appId))
        }
        if (openCollective != null) {
            add(DonateEntity(OPEN_COLLECTIVE_ID, openCollective, appId))
        }
        if (flattrID != null) {
            add(DonateEntity(FLATTR_ID, flattrID, appId))
        }
        if (!donate.isNullOrEmpty()) {
            add(DonateEntity(REGULAR, donate.joinToString(STRING_LIST_SEPARATOR), appId))
        }
    }.ifEmpty { null }
}

fun List<DonateEntity>.toDonation(): Donation {
    var bitcoinAddress: String? = null
    var litecoinAddress: String? = null
    var liberapayId: String? = null
    var openCollectiveId: String? = null
    var flattrId: String? = null
    var regular: List<String>? = null
    for (entity in this) {
        when (entity.type) {
            BITCOIN_ADD -> bitcoinAddress = entity.value
            FLATTR_ID -> flattrId = entity.value
            LIBERAPAY_ID -> liberapayId = entity.value
            LITECOIN_ADD -> litecoinAddress = entity.value
            OPEN_COLLECTIVE_ID -> openCollectiveId = entity.value
            REGULAR -> regular = entity.value.split(STRING_LIST_SEPARATOR)
        }
    }

    return Donation(
        bitcoinAddress = bitcoinAddress,
        litecoinAddress = litecoinAddress,
        liberapayId = liberapayId,
        openCollectiveId = openCollectiveId,
        flattrId = flattrId,
        regularUrl = regular,
    )
}

private const val STRING_LIST_SEPARATOR = "&^%#@!"

@Retention(AnnotationRetention.BINARY)
@IntDef(
    BITCOIN_ADD,
    LITECOIN_ADD,
    LIBERAPAY_ID,
    OPEN_COLLECTIVE_ID,
    FLATTR_ID,
    REGULAR,
)
private annotation class DonationType

private const val BITCOIN_ADD = 0
private const val LITECOIN_ADD = 1
private const val LIBERAPAY_ID = 2
private const val OPEN_COLLECTIVE_ID = 3
private const val FLATTR_ID = 4
private const val REGULAR = 5
