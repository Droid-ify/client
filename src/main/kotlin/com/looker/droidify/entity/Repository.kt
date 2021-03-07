package com.looker.droidify.entity

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.looker.droidify.utility.extension.json.*
import java.net.URL

data class Repository(val id: Long, val address: String, val mirrors: List<String>,
  val name: String, val description: String, val version: Int, val enabled: Boolean,
  val fingerprint: String, val lastModified: String, val entityTag: String,
  val updated: Long, val timestamp: Long, val authentication: String) {
  fun edit(address: String, fingerprint: String, authentication: String): Repository {
    val addressChanged = this.address != address
    val fingerprintChanged = this.fingerprint != fingerprint
    val changed = addressChanged || fingerprintChanged
    return copy(address = address, fingerprint = fingerprint, lastModified = if (changed) "" else lastModified,
      entityTag = if (changed) "" else entityTag, authentication = authentication)
  }

  fun update(mirrors: List<String>, name: String, description: String, version: Int,
    lastModified: String, entityTag: String, timestamp: Long): Repository {
    return copy(mirrors = mirrors, name = name, description = description,
      version = if (version >= 0) version else this.version, lastModified = lastModified,
      entityTag = entityTag, updated = System.currentTimeMillis(), timestamp = timestamp)
  }

  fun enable(enabled: Boolean): Repository {
    return copy(enabled = enabled, lastModified = "", entityTag = "")
  }

  fun serialize(generator: JsonGenerator) {
    generator.writeNumberField("serialVersion", 1)
    generator.writeStringField("address", address)
    generator.writeArray("mirrors") { mirrors.forEach { writeString(it) } }
    generator.writeStringField("name", name)
    generator.writeStringField("description", description)
    generator.writeNumberField("version", version)
    generator.writeBooleanField("enabled", enabled)
    generator.writeStringField("fingerprint", fingerprint)
    generator.writeStringField("lastModified", lastModified)
    generator.writeStringField("entityTag", entityTag)
    generator.writeNumberField("updated", updated)
    generator.writeNumberField("timestamp", timestamp)
    generator.writeStringField("authentication", authentication)
  }

  companion object {
    fun deserialize(id: Long, parser: JsonParser): Repository {
      var address = ""
      var mirrors = emptyList<String>()
      var name = ""
      var description = ""
      var version = 0
      var enabled = false
      var fingerprint = ""
      var lastModified = ""
      var entityTag = ""
      var updated = 0L
      var timestamp = 0L
      var authentication = ""
      parser.forEachKey {
        when {
          it.string("address") -> address = valueAsString
          it.array("mirrors") -> mirrors = collectNotNullStrings()
          it.string("name") -> name = valueAsString
          it.string("description") -> description = valueAsString
          it.number("version") -> version = valueAsInt
          it.boolean("enabled") -> enabled = valueAsBoolean
          it.string("fingerprint") -> fingerprint = valueAsString
          it.string("lastModified") -> lastModified = valueAsString
          it.string("entityTag") -> entityTag = valueAsString
          it.number("updated") -> updated = valueAsLong
          it.number("timestamp") -> timestamp = valueAsLong
          it.string("authentication") -> authentication = valueAsString
          else -> skipChildren()
        }
      }
      return Repository(id, address, mirrors, name, description, version, enabled, fingerprint,
        lastModified, entityTag, updated, timestamp, authentication)
    }

    fun newRepository(address: String, fingerprint: String, authentication: String): Repository {
      val name = try {
        URL(address).let { "${it.host}${it.path}" }
      } catch (e: Exception) {
        address
      }
      return defaultRepository(address, name, "", 0, true, fingerprint, authentication)
    }

    private fun defaultRepository(address: String, name: String, description: String,
      version: Int, enabled: Boolean, fingerprint: String, authentication: String): Repository {
      return Repository(-1, address, emptyList(), name, description, version, enabled,
        fingerprint, "", "", 0L, 0L, authentication)
    }

    val defaultRepositories = listOf(run {
      defaultRepository("https://f-droid.org/repo", "F-Droid", "The official F-Droid Free Software repository. " +
        "Everything in this repository is always built from the source code.",
        21, true, "43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB", "")
    }, run {
      defaultRepository("https://f-droid.org/archive", "F-Droid Archive", "The archive of the official F-Droid Free " +
        "Software repository. Apps here are old and can contain known vulnerabilities and security issues!",
        21, false, "43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB", "")
    }, run {
      defaultRepository("https://guardianproject.info/fdroid/repo", "Guardian Project Official Releases", "The " +
        "official repository of The Guardian Project apps for use with the F-Droid client. Applications in this " +
        "repository are official binaries built by the original application developers and signed by the same key as " +
        "the APKs that are released in the Google Play Store.",
        21, false, "B7C2EEFD8DAC7806AF67DFCD92EB18126BC08312A7F2D6F3862E46013C7A6135", "")
    }, run {
      defaultRepository("https://guardianproject.info/fdroid/archive", "Guardian Project Archive", "The official " +
        "repository of The Guardian Project apps for use with the F-Droid client. This contains older versions of " +
        "applications from the main repository.", 21, false,
        "B7C2EEFD8DAC7806AF67DFCD92EB18126BC08312A7F2D6F3862E46013C7A6135", "")
    }, run {
      defaultRepository("https://apt.izzysoft.de/fdroid/repo", "IzzyOnDroid F-Droid Repo", "This is a " +
              "repository of apps to be used with F-Droid the original application developers, taken from the resp. " +
              "repositories (mostly GitHub). At this moment I cannot give guarantees on regular updates for all of them, " +
              "though most are checked multiple times a week ", 21, true,
              "3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A", "")
    })
  }
}
