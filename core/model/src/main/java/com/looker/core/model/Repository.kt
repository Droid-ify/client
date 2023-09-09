package com.looker.core.model

import java.net.URL

data class Repository(
	var id: Long,
	val address: String,
	val mirrors: List<String>,
	val name: String,
	val description: String,
	val version: Int,
	val enabled: Boolean,
	val fingerprint: String,
	val lastModified: String,
	val entityTag: String,
	val updated: Long,
	val timestamp: Long,
	val authentication: String
) {
	fun edit(address: String, fingerprint: String, authentication: String): Repository {
		val isAddressChanged = this.address != address
		val isFingerprintChanged = this.fingerprint != fingerprint
		val shouldForceUpdate = isAddressChanged || isFingerprintChanged
		return copy(
			address = address,
			fingerprint = fingerprint,
			lastModified = if (shouldForceUpdate) "" else lastModified,
			entityTag = if (shouldForceUpdate) "" else entityTag,
			authentication = authentication
		)
	}

	fun update(
		mirrors: List<String>,
		name: String,
		description: String,
		version: Int,
		lastModified: String,
		entityTag: String,
		timestamp: Long,
	): Repository {
		return copy(
			mirrors = mirrors,
			name = name,
			description = description,
			version = if (version >= 0) version else this.version,
			lastModified = lastModified,
			entityTag = entityTag,
			updated = System.currentTimeMillis(),
			timestamp = timestamp
		)
	}

	fun enable(enabled: Boolean): Repository {
		return copy(enabled = enabled, lastModified = "", entityTag = "")
	}

	@Suppress("SpellCheckingInspection")
	companion object {

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
			address: String,
			name: String,
			description: String,
			version: Int = 21,
			enabled: Boolean = false,
			fingerprint: String,
			authentication: String = ""
		): Repository {
			return Repository(
				-1, address, emptyList(), name, description, version, enabled,
				fingerprint, "", "", 0L, 0L, authentication
			)
		}

		val defaultRepositories = listOf(
			defaultRepository(
				address = "https://f-droid.org/repo",
				name = "F-Droid",
				description = "The official F-Droid Free Software repository. Everything in this repository is always built from the source code.",
				enabled = true,
				fingerprint = "43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB"
			),
			defaultRepository(
				address = "https://f-droid.org/archive",
				name = "F-Droid Archive",
				description = "The archive of the official F-Droid Free Software repository. Apps here are old and can contain known vulnerabilities and security issues!",
				fingerprint = "43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB"
			),
			defaultRepository(
				address = "https://guardianproject.info/fdroid/repo",
				name = "Guardian Project Official Releases",
				description = "The official repository of The Guardian Project apps for use with the F-Droid client. Applications in this repository are official binaries built by the original application developers and signed by the same key as the APKs that are released in the Google Play Store.",
				fingerprint = "B7C2EEFD8DAC7806AF67DFCD92EB18126BC08312A7F2D6F3862E46013C7A6135"
			),
			defaultRepository(
				address = "https://guardianproject.info/fdroid/archive",
				name = "Guardian Project Archive",
				description = "The official repository of The Guardian Project apps for use with the F-Droid client. This contains older versions of applications from the main repository.",
				fingerprint = "B7C2EEFD8DAC7806AF67DFCD92EB18126BC08312A7F2D6F3862E46013C7A6135"
			),
			defaultRepository(
				address = "https://apt.izzysoft.de/fdroid/repo",
				name = "IzzyOnDroid F-Droid Repo",
				description = "This is a repository of apps to be used with F-Droid the original application developers, taken from the resp. repositories (mostly GitHub). At this moment I cannot give guarantees on regular updates for all of them, though most are checked multiple times a week ",
				enabled = true,
				fingerprint = "3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A"
			),
			defaultRepository(
				address = "https://microg.org/fdroid/repo",
				name = "MicroG Project",
				description = "The official repository for MicroG. MicroG is a lightweight open-source implementation of Google Play Services.",
				fingerprint = "9BD06727E62796C0130EB6DAB39B73157451582CBD138E86C468ACC395D14165"
			),
			defaultRepository(
				address = "https://repo.netsyms.com/fdroid/repo",
				name = "Netsyms Technologies",
				description = "Official collection of open-source apps created by Netsyms Technologies.",
				fingerprint = "2581BA7B32D3AB443180C4087CAB6A7E8FB258D3A6E98870ECB3C675E4D64489"
			),
			defaultRepository(
				address = "https://fdroid.bromite.org/fdroid/repo",
				name = "Bromite",
				description = "The official repository for Bromite. Bromite is a Chromium with ad blocking and enhanced privacy.",
				fingerprint = "E1EE5CD076D7B0DC84CB2B45FB78B86DF2EB39A3B6C56BA3DC292A5E0C3B9504"
			),
			defaultRepository(
				address = "https://molly.im/fdroid/foss/fdroid/repo",
				name = "Molly",
				description = "The official repository for Molly. Molly is a fork of Signal focused on security.",
				fingerprint = "5198DAEF37FC23C14D5EE32305B2AF45787BD7DF2034DE33AD302BDB3446DF74"
			),
			defaultRepository(
				address = "https://archive.newpipe.net/fdroid/repo",
				name = "NewPipe",
				description = "The official repository for NewPipe. NewPipe is a lightweight client for Youtube, PeerTube, Soundcloud, etc.",
				fingerprint = "E2402C78F9B97C6C89E97DB914A2751FDA1D02FE2039CC0897A462BDB57E7501"
			),
			defaultRepository(
				address = "https://www.collaboraoffice.com/downloads/fdroid/repo",
				name = "Collabora Office",
				description = "Collabora Office is an office suite based on LibreOffice.",
				fingerprint = "573258C84E149B5F4D9299E7434B2B69A8410372921D4AE586BA91EC767892CC"
			),
			defaultRepository(
				address = "https://fdroid.libretro.com/repo",
				name = "LibRetro",
				description = "The official canary repository for this great retro emulators hub.",
				fingerprint = "3F05B24D497515F31FEAB421297C79B19552C5C81186B3750B7C131EF41D733D"
			),
			defaultRepository(
				address = "https://cdn.kde.org/android/fdroid/repo",
				name = "KDE Android",
				description = "The official nightly repository for KDE Android apps.",
				fingerprint = "B3EBE10AFA6C5C400379B34473E843D686C61AE6AD33F423C98AF903F056523F"
			),
			defaultRepository(
				address = "https://calyxos.gitlab.io/calyx-fdroid-repo/fdroid/repo",
				name = "Calyx OS Repo",
				description = "The official Calyx Labs F-Droid repository.",
				fingerprint = "C44D58B4547DE5096138CB0B34A1CC99DAB3B4274412ED753FCCBFC11DC1B7B6"
			),
			defaultRepository(
				address = "https://divestos.org/fdroid/official",
				name = "Divest OS Repo",
				description = "The official Divest OS F-Droid repository.",
				fingerprint = "E4BE8D6ABFA4D9D4FEEF03CDDA7FF62A73FD64B75566F6DD4E5E577550BE8467"
			),
			defaultRepository(
				address = "https://fdroid.fedilab.app/repo",
				name = "Fedilab",
				description = "The official repository for Fedilab. Fedilab is a multi-accounts client for Mastodon, Peertube, and other free software social networks.",
				fingerprint = "11F0A69910A4280E2CD3CCC3146337D006BE539B18E1A9FEACE15FF757A94FEB"
			),
			defaultRepository(
				address = "https://store.nethunter.com/repo",
				name = "Kali Nethunter",
				description = "Kali Nethunter's official selection of original binaries.",
				fingerprint = "7E418D34C3AD4F3C37D7E6B0FACE13332364459C862134EB099A3BDA2CCF4494"
			),
			defaultRepository(
				address = "https://secfirst.org/fdroid/repo",
				name = "Umbrella",
				description = "The official repository for Umbrella. Umbrella is a collection of security advices, tutorials, tools etc..",
				fingerprint = "39EB57052F8D684514176819D1645F6A0A7BD943DBC31AB101949006AC0BC228"
			),
			defaultRepository(
				address = "https://thecapslock.gitlab.io/fdroid-patched-apps/fdroid/repo",
				name = "Patched Apps",
				description = "A collection of patched applications to provide better compatibility, privacy etc..",
				fingerprint = "313D9E6E789FF4E8E2D687AAE31EEF576050003ED67963301821AC6D3763E3AC"
			),
			defaultRepository(
				address = "https://mobileapp.bitwarden.com/fdroid/repo",
				name = "Bitwarden",
				description = "The official repository for Bitwarden. Bitwarden is a password manager.",
				fingerprint = "BC54EA6FD1CD5175BCCCC47C561C5726E1C3ED7E686B6DB4B18BAC843A3EFE6C"
			),
			defaultRepository(
				address = "https://briarproject.org/fdroid/repo",
				name = "Briar",
				description = "The official repository for Briar. Briar is a serverless/offline messenger that focused on privacy, security, and decentralization.",
				fingerprint = "1FB874BEE7276D28ECB2C9B06E8A122EC4BCB4008161436CE474C257CBF49BD6"
			),
			defaultRepository(
				address = "https://guardianproject-wind.s3.amazonaws.com/fdroid/repo",
				name = "Wind Project",
				description = "A collection of interesting offline/serverless apps.",
				fingerprint = "182CF464D219D340DA443C62155198E399FEC1BC4379309B775DD9FC97ED97E1"
			),
			defaultRepository(
				address = "https://nanolx.org/fdroid/repo",
				name = "NanoDroid",
				description = "A companion repository to microG's installer.",
				fingerprint = "862ED9F13A3981432BF86FE93D14596B381D75BE83A1D616E2D44A12654AD015"
			),
			defaultRepository(
				address = "https://fluffychat.im/repo/stable/repo",
				name = "FluffyChat",
				description = "The official repository for FluffyChat. Fluffychat is a Matrix client.",
				fingerprint = "8E2637AEF6697CC6DD486AF044A6EE45B1A742AE3EF56566E748CDE8BC65C1FB"
			),
			defaultRepository(
				address = "https://fluffychat.im/repo/nightly/repo",
				name = "FluffyChat Nightly",
				description = "The official nightly repository for FluffyChat.",
				fingerprint = "21A469657300576478B623DF99D8EB889A80BCD939ACA60A4074741BEAEC397D"
			),
			defaultRepository(
				address = "https://releases.threema.ch/fdroid/repo",
				name = "Threema Libre",
				description = "The official repository for Threema Libre. Requires Threema Shop license. Threema Libre is an open-source messanger focused on security and privacy.",
				fingerprint = "5734E753899B25775D90FE85362A49866E05AC4F83C05BEF5A92880D2910639E"
			),
			defaultRepository(
				address = "https://fdroid.getsession.org/fdroid/repo",
				name = "Session",
				description = "The official repository for Session. Session is an open-source messanger focused on security and privacy.",
				fingerprint = "DB0E5297EB65CC22D6BD93C869943BDCFCB6A07DC69A48A0DD8C7BA698EC04E6"
			)
		)
	}
}
