package com.looker.droidify.entity

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.looker.droidify.utility.extension.json.collectNotNullStrings
import com.looker.droidify.utility.extension.json.forEachKey
import com.looker.droidify.utility.extension.json.writeArray
import java.net.URL

data class Repository(
	var id: Long, val address: String, val mirrors: List<String>,
	val name: String, val description: String, val version: Int, val enabled: Boolean,
	val fingerprint: String, val lastModified: String, val entityTag: String,
	val updated: Long, val timestamp: Long, val authentication: String,
) {
	fun edit(address: String, fingerprint: String, authentication: String): Repository {
		val addressChanged = this.address != address
		val fingerprintChanged = this.fingerprint != fingerprint
		val changed = addressChanged || fingerprintChanged
		return copy(
			address = address,
			fingerprint = fingerprint,
			lastModified = if (changed) "" else lastModified,
			entityTag = if (changed) "" else entityTag,
			authentication = authentication
		)
	}

	fun update(
		mirrors: List<String>, name: String, description: String, version: Int,
		lastModified: String, entityTag: String, timestamp: Long,
	): Repository {
		return copy(
			mirrors = mirrors, name = name, description = description,
			version = if (version >= 0) version else this.version, lastModified = lastModified,
			entityTag = entityTag, updated = System.currentTimeMillis(), timestamp = timestamp
		)
	}

	fun enable(enabled: Boolean): Repository {
		return copy(enabled = enabled, lastModified = "", entityTag = "")
	}

	fun serialize(generator: JsonGenerator) {
		generator.writeNumberField("serialVersion", 1)
		generator.writeNumberField("id", id)
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
		fun deserialize(parser: JsonParser): Repository {
			var id = 0L
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
					it.string("id") -> id = valueAsLong
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
			return Repository(
				id, address, mirrors, name, description, version, enabled, fingerprint,
				lastModified, entityTag, updated, timestamp, authentication
			)
		}

		fun newRepository(
			address: String,
			fingerprint: String,
			authentication: String,
		): Repository {
			val name = try {
				URL(address).let { "${it.host}${it.path}" }
			} catch (e: Exception) {
				address
			}
			return defaultRepository(address, name, "", 0, true, fingerprint, authentication)
		}

		private fun defaultRepository(
			address: String, name: String, description: String,
			version: Int, enabled: Boolean, fingerprint: String, authentication: String,
		): Repository {
			return Repository(
				-1, address, emptyList(), name, description, version, enabled,
				fingerprint, "", "", 0L, 0L, authentication
			)
		}

		val defaultRepositories = listOf(run {
			defaultRepository(
				"https://f-droid.org/repo",
				"F-Droid",
				"The official F-Droid Free Software repository. " +
						"Everything in this repository is always built from the source code.",
				21,
				true,
				"43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB",
				""
			)
		}, run {
			defaultRepository(
				"https://f-droid.org/archive",
				"F-Droid Archive",
				"The archive of the official F-Droid Free " +
						"Software repository. Apps here are old and can contain known vulnerabilities and security issues!",
				21,
				false,
				"43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB",
				""
			)
		}, run {
			defaultRepository(
				"https://guardianproject.info/fdroid/repo",
				"Guardian Project Official Releases",
				"The " +
						"official repository of The Guardian Project apps for use with the F-Droid client. Applications in this " +
						"repository are official binaries built by the original application developers and signed by the same key as " +
						"the APKs that are released in the Google Play Store.",
				21,
				false,
				"B7C2EEFD8DAC7806AF67DFCD92EB18126BC08312A7F2D6F3862E46013C7A6135",
				""
			)
		}, run {
			defaultRepository(
				"https://guardianproject.info/fdroid/archive",
				"Guardian Project Archive",
				"The official " +
						"repository of The Guardian Project apps for use with the F-Droid client. This contains older versions of " +
						"applications from the main repository.",
				21,
				false,
				"B7C2EEFD8DAC7806AF67DFCD92EB18126BC08312A7F2D6F3862E46013C7A6135",
				""
			)
		}, run {
			defaultRepository(
				"https://apt.izzysoft.de/fdroid/repo", "IzzyOnDroid F-Droid Repo", "This is a " +
						"repository of apps to be used with F-Droid the original application developers, taken from the resp. " +
						"repositories (mostly GitHub). At this moment I cannot give guarantees on regular updates for all of them, " +
						"though most are checked multiple times a week ", 21, true,
				"3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A", ""
			)
		}, run {
			defaultRepository(
				"https://microg.org/fdroid/repo", "MicroG Project",
				"Official repository of the open-source implementation of Google Play Services.",
				21, false, "9BD06727E62796C0130EB6DAB39B73157451582CBD138E86C468ACC395D14165", ""
			)
		}, run {
			defaultRepository(
				"https://repo.netsyms.com/fdroid/repo", "Netsyms Technologies",
				"Open-source apps created by Netsyms Technologies.",
				21, false, "2581BA7B32D3AB443180C4087CAB6A7E8FB258D3A6E98870ECB3C675E4D64489", ""
			)
		}, run {
			defaultRepository(
				"https://fdroid.bromite.org/fdroid/repo", "Bromite",
				"Bromite is a Chromium plus ad blocking and enhanced privacy; take back your browser.",
				21, false, "E1EE5CD076D7B0DC84CB2B45FB78B86DF2EB39A3B6C56BA3DC292A5E0C3B9504", ""
			)
		}, run {
			defaultRepository(
				"https://molly.im/fdroid/foss/fdroid/repo", "Molly",
				"Molly is a fork of Signal focused on security.",
				21, false, "5198DAEF37FC23C14D5EE32305B2AF45787BD7DF2034DE33AD302BDB3446DF74", ""
			)
		}, run {
			defaultRepository(
				"https://archive.newpipe.net/fdroid/repo", "NewPipe",
				"NewPipe's official independent repository.",
				21, false, "E2402C78F9B97C6C89E97DB914A2751FDA1D02FE2039CC0897A462BDB57E7501", ""
			)
		}, run {
			defaultRepository(
				"https://www.collaboraoffice.com/downloads/fdroid/repo", "Collabora Office",
				"Collabora Office is an office suite based on LibreOffice.",
				21, false, "573258C84E149B5F4D9299E7434B2B69A8410372921D4AE586BA91EC767892CC", ""
			)
		}, run {
			defaultRepository(
				"https://www.droidware.info/fdroid/repo", "Ungoogled Chromium",
				"Chromium sans dependency on Google web services. It also features some enhancments to privacy, control & transparency",
				21, false, "2144449AB1DD270EC31B6087409B5D0EA39A75A9F290DA62AC1B238A0EAAF851", ""
			)
		}, run {
			defaultRepository(
				"https://fdroid.libretro.com/repo", "LibRetro",
				"The official canary repository for this great retro emulators hub.",
				21, false, "3F05B24D497515F31FEAB421297C79B19552C5C81186B3750B7C131EF41D733D", ""
			)
		}, run {
			defaultRepository(
				"https://cdn.kde.org/android/fdroid/repo", "KDE Android",
				"The official nightly repository for KDE Android apps.",
				21, false, "B3EBE10AFA6C5C400379B34473E843D686C61AE6AD33F423C98AF903F056523F", ""
			)
		}, run {
			defaultRepository(
				"https://rfc2822.gitlab.io/fdroid-firefox/fdroid/repo", "Unofficial Firefox",
				"An unofficial repository with some of the most well known FOSS apps not on F-Droid.",
				21, false, "8F992BBBA0340EFE6299C7A410B36D9C8889114CA6C58013C3587CDA411B4AED", ""
			)
		}, run {
			defaultRepository(
				"https://calyxos.gitlab.io/calyx-fdroid-repo/fdroid/repo", "Calyx OS Repo",
				"The official Calyx Labs F-Droid repository.",
				21, false, "C44D58B4547DE5096138CB0B34A1CC99DAB3B4274412ED753FCCBFC11DC1B7B6", ""
			)
		}, run {
			defaultRepository(
				"https://divestos.org/fdroid/official", "Divest OS Repo",
				"The official Divest OS F-Droid repository.",
				21, false, "E4BE8D6ABFA4D9D4FEEF03CDDA7FF62A73FD64B75566F6DD4E5E577550BE8467", ""
			)
		}, run {
			defaultRepository(
				"https://fdroid.fedilab.app/repo", "Fedilab",
				"Fedilab's official F-Droid repository.",
				21, false, "11F0A69910A4280E2CD3CCC3146337D006BE539B18E1A9FEACE15FF757A94FEB", ""
			)
		}, run {
			defaultRepository(
				"https://store.nethunter.com/repo", "Kali Nethunter",
				"Kali Nethunter's official selection of original binaries.",
				21, false, "7E418D34C3AD4F3C37D7E6B0FACE13332364459C862134EB099A3BDA2CCF4494", ""
			)
		}, run {
			defaultRepository(
				"https://secfirst.org/fdroid/repo", "Umbrella",
				"Security advices, tutorials, tools etc..",
				21, false, "39EB57052F8D684514176819D1645F6A0A7BD943DBC31AB101949006AC0BC228", ""
			)
		}, run {
			defaultRepository(
				"https://thecapslock.gitlab.io/fdroid-patched-apps/fdroid/repo", "Patched Apps",
				"A collection of patched applications to provide better compatibility, privacy etc..",
				21, false, "313D9E6E789FF4E8E2D687AAE31EEF576050003ED67963301821AC6D3763E3AC", ""
			)
		}, run {
			defaultRepository(
				"https://mobileapp.bitwarden.com/fdroid/repo", "Bitwarden",
				"The official repository for Bitwarden.",
				21, false, "BC54EA6FD1CD5175BCCCC47C561C5726E1C3ED7E686B6DB4B18BAC843A3EFE6C", ""
			)
		}, run {
			defaultRepository(
				"https://briarproject.org/fdroid/repo", "Briar",
				"An serverless/offline messenger that hides your metadata.",
				21, false, "1FB874BEE7276D28ECB2C9B06E8A122EC4BCB4008161436CE474C257CBF49BD6", ""
			)
		}, run {
			defaultRepository(
				"https://guardianproject-wind.s3.amazonaws.com/fdroid/repo", "Wind Project",
				"A collection of interesting offline/serverless apps.",
				21, false, "182CF464D219D340DA443C62155198E399FEC1BC4379309B775DD9FC97ED97E1", ""
			)
		}, run {
			defaultRepository(
				"https://repo.alefvanoon.xyz/fdroid/repo", "Alefvanoon",
				"A collection of open-source apps that for one reason or another not on F-Droid.",
				21, false, "04DF198F553069C7BE60F057AE12000E99F7700DA895CC1CE2EB11DC871581F1", ""
			)
		}, run {
			defaultRepository(
				"https://nanolx.org/fdroid/repo", "NanoDroid",
				"A companion repository to microG's installer.",
				21, false, "862ED9F13A3981432BF86FE93D14596B381D75BE83A1D616E2D44A12654AD015", ""
			)
		})
	}
}
