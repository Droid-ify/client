package com.looker.droidify.data.local

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.looker.droidify.data.encryption.Encrypted
import com.looker.droidify.data.local.sql.Anti_features_app_relation
import com.looker.droidify.data.local.sql.Authentication
import com.looker.droidify.data.local.sql.Donate
import com.looker.droidify.data.local.sql.DroidifyDb
import com.looker.droidify.data.local.sql.Graphic
import com.looker.droidify.data.local.sql.Permission
import com.looker.droidify.data.local.sql.Repository
import com.looker.droidify.data.local.sql.Screenshot
import com.looker.droidify.data.local.sql.Version
import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.sync.JsonParser
import com.looker.droidify.sync.v2.model.LocalizedString

private val localizedStringAdapter = object : ColumnAdapter<LocalizedString, String> {
    override fun decode(databaseValue: String): LocalizedString =
        JsonParser.decodeFromString(databaseValue)

    override fun encode(value: LocalizedString): String =
        JsonParser.encodeToString(value)
}

@OptIn(ExperimentalStdlibApi::class)
private val fingerprintAdapter = object : ColumnAdapter<Fingerprint, ByteArray> {
    override fun decode(databaseValue: ByteArray): Fingerprint = Fingerprint(databaseValue)

    override fun encode(value: Fingerprint): ByteArray = value.value.hexToByteArray()
}

private val encryptedAdapter = object : ColumnAdapter<Encrypted, String> {
    override fun decode(databaseValue: String): Encrypted = Encrypted(databaseValue)

    override fun encode(value: Encrypted): String = value.value
}

fun droidifyDb(driver: SqlDriver): DroidifyDb = DroidifyDb(
    driver = driver,
    anti_features_app_relationAdapter = Anti_features_app_relation.Adapter(
        reasonAdapter = localizedStringAdapter,
    ),
    authenticationAdapter = Authentication.Adapter(passwordAdapter = encryptedAdapter),
    donateAdapter = Donate.Adapter(typeAdapter = IntColumnAdapter),
    graphicAdapter = Graphic.Adapter(typeAdapter = IntColumnAdapter),
    permissionAdapter = Permission.Adapter(maxSdkVersionAdapter = IntColumnAdapter),
    repositoryAdapter = Repository.Adapter(fingerprintAdapter = fingerprintAdapter),
    screenshotAdapter = Screenshot.Adapter(typeAdapter = IntColumnAdapter),
    versionAdapter = Version.Adapter(
        whatsNewAdapter = localizedStringAdapter,
        maxSdkVersionAdapter = IntColumnAdapter,
        minSdkVersionAdapter = IntColumnAdapter,
        targetSdkVersionAdapter = IntColumnAdapter,
    ),
)
