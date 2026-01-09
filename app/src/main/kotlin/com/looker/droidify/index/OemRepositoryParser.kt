package com.looker.droidify.index

import android.util.Xml
import com.looker.droidify.data.encryption.sha256
import com.looker.droidify.data.model.hex
import com.looker.droidify.model.Repository
import com.looker.droidify.model.Repository.Companion.defaultRepository
import java.io.File
import java.io.InputStream
import org.xmlpull.v1.XmlPullParser

/**
 * Direct copy of implementation from https://github.com/NeoApplications/Neo-Store/blob/master/src/main/kotlin/com/machiav3lli/fdroid/data/database/entity/Repository.kt
 * */
object OemRepositoryParser {

    private val rootDirs = arrayOf("/system", "/product", "/vendor", "/odm", "/oem")
    private val supportedPackageNames = arrayOf("com.looker.droidify", "org.fdroid.fdroid")
    private const val FILE_NAME = "additional_repos.xml"

    fun getSystemDefaultRepos() = rootDirs.flatMap { rootDir ->
        supportedPackageNames.map { packageName -> "$rootDir/etc/$packageName/$FILE_NAME" }
    }.flatMap { path ->
        val file = File(path)
        if (file.exists()) parse(file.inputStream()) else emptyList()
    }.takeIf { it.isNotEmpty() }

    fun parse(inputStream: InputStream): List<Repository> = with(Xml.newPullParser()) {
        val repoItems = mutableListOf<String>()
        inputStream.use { input ->
            setInput(input, null)
            var isItem = false
            while (next() != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> if (name == "item") isItem = true
                    XmlPullParser.TEXT -> if (isItem) repoItems.add(text)
                    XmlPullParser.END_TAG -> isItem = false
                }
            }
        }
        repoItems.chunked(7).mapNotNull { itemsSet -> fromXML(itemsSet) }
    }

    private fun fromXML(xml: List<String>) = runCatching {
        defaultRepository(
            name = xml[0],
            address = xml[1],
            description = xml[2].replace(Regex("\\s+"), " ").trim(),
            version = xml[3].toInt(),
            enabled = xml[4].toInt() > 0,
            fingerprint = xml[6].let {
                if (it.length > 32) {
                    val encoded = it
                        .chunked(2)
                        .mapNotNull { byteStr ->
                            try {
                                byteStr.toInt(16).toByte()
                            } catch (_: NumberFormatException) {
                                null
                            }
                        }
                        .toByteArray()
                    sha256(encoded).hex()
                } else it
            },
            authentication = "",
        )
    }.getOrNull()
}
