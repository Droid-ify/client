package com.looker.droidify.data.local

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.looker.droidify.data.encryption.Encrypted
import com.looker.droidify.data.local.sql.Authentication
import com.looker.droidify.data.local.sql.Donate
import com.looker.droidify.data.local.sql.DroidifyDb
import com.looker.droidify.data.local.sql.Graphic
import com.looker.droidify.data.local.sql.Permission
import com.looker.droidify.data.local.sql.Repository
import com.looker.droidify.data.local.sql.Screenshot
import com.looker.droidify.data.local.sql.Version
import com.looker.droidify.data.model.Fingerprint

fun droidifyDb(driver: SqlDriver) = DroidifyDb(
    driver = driver,
    repositoryAdapter = Repository.Adapter(fingerprintAdapter),
    authenticationAdapter = Authentication.Adapter(encryptedAdapter),
    donateAdapter = Donate.Adapter(EnumColumnAdapter()),
    graphicAdapter = Graphic.Adapter(EnumColumnAdapter()),
    screenshotAdapter = Screenshot.Adapter(EnumColumnAdapter()),
    permissionAdapter = Permission.Adapter(IntColumnAdapter),
    versionAdapter = Version.Adapter(IntColumnAdapter, IntColumnAdapter, IntColumnAdapter),
)

@OptIn(ExperimentalStdlibApi::class)
private val fingerprintAdapter = object : ColumnAdapter<Fingerprint, ByteArray> {
    override fun decode(databaseValue: ByteArray): Fingerprint = Fingerprint(databaseValue)
    override fun encode(value: Fingerprint): ByteArray = value.bytes()
}

private val encryptedAdapter = object : ColumnAdapter<Encrypted, String> {
    override fun decode(databaseValue: String): Encrypted = Encrypted(databaseValue)
    override fun encode(value: Encrypted): String = value.value
}
