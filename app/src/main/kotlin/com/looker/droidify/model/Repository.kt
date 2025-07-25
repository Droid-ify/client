package com.looker.droidify.model

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
    val authentication: String,
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

        fun defaultRepository(
            address: String,
            name: String,
            description: String,
            version: Int = 21,
            enabled: Boolean = false,
            fingerprint: String,
            authentication: String = "",
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
                description = "The official F-Droid Free Software repos" +
                    "itory. Everything in this repository is always buil" +
                    "t from the source code.",
                enabled = true,
                fingerprint = "43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB"
            ),
            defaultRepository(
                address = "https://f-droid.org/archive",
                name = "F-Droid Archive",
                description = "The archive of the official F-Droid Free" +
                    " Software repository. Apps here are old and can co" +
                    "ntain known vulnerabilities and security issues!",
                fingerprint = "43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB"
            ),
            defaultRepository(
                address = "https://guardianproject.info/fdroid/repo",
                name = "Guardian Project official releases",
                description = "The official repository of The Guardian " +
                    "Project apps for use with the F-Droid client. Appl" +
                    "ications in this repository are official binaries " +
                    "built by the original application developers and " +
                    "signed by the same key as the APKs that are relea" +
                    "sed in the Google Play Store.",
                fingerprint = "B7C2EEFD8DAC7806AF67DFCD92EB18126BC08312A7F2D6F3862E46013C7A6135"
            ),
            defaultRepository(
                address = "https://guardianproject.info/fdroid/archive",
                name = "Guardian Project Archive",
                description = "The official repository of The Guardian Pr" +
                    "oject apps for use with the F-Droid client. This con" +
                    "tains older versions of applications from the main repository.",
                fingerprint = "B7C2EEFD8DAC7806AF67DFCD92EB18126BC08312A7F2D6F3862E46013C7A6135"
            ),
            defaultRepository(
                address = "https://apt.izzysoft.de/fdroid/repo",
                name = "IzzyOnDroid F-Droid repository",
                description = "This is a repository of apps to be used with" +
                    " F-Droid the original application developers, taken" +
                    " from the responsible repositories. At thi" +
                    "s moment I cannot give guarantees on regular updates" +
                    " for all of them, though most are checked multiple times a week.",
                enabled = true,
                fingerprint = "3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A"
            ),
            defaultRepository(
                address = "https://microg.org/fdroid/repo",
                name = "microG Project",
                description = "The official repository for microG." +
                    " microG is a lightweight open source implementation" +
                    " of Google Play Services.",
                fingerprint = "9BD06727E62796C0130EB6DAB39B73157451582CBD138E86C468ACC395D14165"
            ),
            defaultRepository(
                address = "https://repo.netsyms.com/fdroid/repo",
                name = "Netsyms Technologies",
                description = "Official collection of open-source apps created" +
                    " by Netsyms Technologies.",
                fingerprint = "2581BA7B32D3AB443180C4087CAB6A7E8FB258D3A6E98870ECB3C675E4D64489"
            ),
            defaultRepository(
                address = "https://molly.im/fdroid/foss/fdroid/repo",
                name = "Molly",
                description = "The official repository for Molly. " +
                    "Molly is a fork of Signal focused on security.",
                fingerprint = "5198DAEF37FC23C14D5EE32305B2AF45787BD7DF2034DE33AD302BDB3446DF74"
            ),
            defaultRepository(
                address = "https://archive.newpipe.net/fdroid/repo",
                name = "NewPipe",
                description = "The official repository for NewPipe." +
                    " NewPipe is a lightweight client for YouTube, PeerTube" +
                    ", Soundcloud, etc.",
                fingerprint = "E2402C78F9B97C6C89E97DB914A2751FDA1D02FE2039CC0897A462BDB57E7501"
            ),
            defaultRepository(
                address = "https://www.collaboraoffice.com/downloads/fdroid/repo",
                name = "Collabora Office",
                description = "Collabora Office is an office suite based on LibreOffice.",
                fingerprint = "573258C84E149B5F4D9299E7434B2B69A8410372921D4AE586BA91EC767892CC"
            ),
            defaultRepository(
                address = "https://cdn.kde.org/android/fdroid/repo",
                name = "KDE Android",
                description = "The official nightly repository for KDE Android apps.",
                fingerprint = "B3EBE10AFA6C5C400379B34473E843D686C61AE6AD33F423C98AF903F056523F"
            ),
            defaultRepository(
                address = "https://calyxos.gitlab.io/calyx-fdroid-repo/fdroid/repo",
                name = "CalyxOS repository",
                description = "The official Calyx Labs F-Droid repository.",
                fingerprint = "C44D58B4547DE5096138CB0B34A1CC99DAB3B4274412ED753FCCBFC11DC1B7B6"
            ),
            defaultRepository(
                address = "https://fdroid.fedilab.app/repo",
                name = "Fedilab",
                description = "The official repository for Fedilab. Fedilab is a " +
                    "multi-accounts client for Mastodon, PeerTube, and other free" +
                    " software social networks.",
                fingerprint = "11F0A69910A4280E2CD3CCC3146337D006BE539B18E1A9FEACE15FF757A94FEB"
            ),
            defaultRepository(
                address = "https://store.nethunter.com/repo",
                name = "Kali Nethunter",
                description = "Kali Nethunter's official selection of original b" +
                    "inaries.",
                fingerprint = "FE7A23DFC003A1CF2D2ADD2469B9C0C49B206BA5DC9EDD6563B3B7EB6A8F5FAB"
            ),
            defaultRepository(
                address = "https://thecapslock.gitlab.io/fdroid-patched-apps/fdroid/repo",
                name = "Patched Apps",
                description = "A collection of patched applications to provid" +
                    "e better compatibility, privacy etc.",
                fingerprint = "313D9E6E789FF4E8E2D687AAE31EEF576050003ED67963301821AC6D3763E3AC"
            ),
            defaultRepository(
                address = "https://mobileapp.bitwarden.com/fdroid/repo",
                name = "Bitwarden",
                description = "The official repository for Bitwarden. Bitward" +
                    "en is a password manager.",
                fingerprint = "BC54EA6FD1CD5175BCCCC47C561C5726E1C3ED7E686B6DB4B18BAC843A3EFE6C"
            ),
            defaultRepository(
                address = "https://briarproject.org/fdroid/repo",
                name = "Briar",
                description = "The official repository for Briar. Briar is a" +
                    " serverless/offline messenger that focused on privacy, s" +
                    "ecurity, and decentralization.",
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
                address = "https://releases.threema.ch/fdroid/repo",
                name = "Threema Libre",
                description = "The official repository for Threema Libre. R" +
                    "equires Threema Shop license. Threema Libre is an open" +
                    "source messenger focused on security and privacy.",
                fingerprint = "5734E753899B25775D90FE85362A49866E05AC4F83C05BEF5A92880D2910639E"
            ),
            defaultRepository(
                address = "https://fdroid.getsession.org/fdroid/repo",
                name = "Session",
                description = "The official repository for Session. Session" +
                    " is an open source messenger focused on security and privacy.",
                fingerprint = "DB0E5297EB65CC22D6BD93C869943BDCFCB6A07DC69A48A0DD8C7BA698EC04E6"
            ),
            defaultRepository(
                address = "https://www.cromite.org/fdroid/repo",
                name = "Cromite",
                description = "The official repository for Cromite. Cromite" +
                    " is a Chromium with ad blocking and enhanced privacy.",
                fingerprint = "49F37E74DEE483DCA2B991334FB5A0200787430D0B5F9A783DD5F13695E9517B"
            ),
            defaultRepository(
                address = "https://fdroid.twinhelix.com/fdroid/repo",
                name = "TwinHelix",
                description = "TwinHelix F-Droid Repository, used for Signa" +
                    "l-FOSS, an open-source fork of Signal Private Messenger.",
                fingerprint = "7b03b0232209b21b10a30a63897d3c6bca4f58fe29bc3477e8e3d8cf8e304028"
            ),
            defaultRepository(
                address = "https://fdroid.typeblog.net",
                name = "PeterCxy's F-Droid",
                description = "You have landed on PeterCxy's F-Droid repo. T" +
                    "o use this repository, please add the page's URL to your F-Droid client.",
                fingerprint = "1a7e446c491c80bc2f83844a26387887990f97f2f379ae7b109679feae3dbc8c"
            ),
            defaultRepository(
                address = "https://s2.spiritcroc.de/fdroid/repo",
                name = "SpiritCroc.de",
                description = "While some of my apps are available from" +
                    " the official F-Droid repository, I also maintain my" +
                    " own repository for a small selection of apps. These" +
                    " might be forks of other apps with only minor change" +
                    "s, or apps that are not published on the Play Store f" +
                    "or other reasons. In contrast to the official F-Droid" +
                    " repos, these might also include proprietary librarie" +
                    "s, e.g. for push notifications.",
                fingerprint = "6612ade7e93174a589cf5ba26ed3ab28231a789640546c8f30375ef045bc9242"
            ),
            defaultRepository(
                address = "https://s2.spiritcroc.de/testing/fdroid/repo",
                name = "SpiritCroc.de test builds",
                description = "SpiritCroc.de test builds",
                fingerprint = "52d03f2fab785573bb295c7ab270695e3a1bdd2adc6a6de8713250b33f231225"
            ),
            defaultRepository(
                address = "https://static.cryptomator.org/android/fdroid/repo",
                name = "Cryptomator",
                description = "No description.",
                fingerprint = "f7c3ec3b0d588d3cb52983e9eb1a7421c93d4339a286398e71d7b651e8d8ecdd"
            ),
            defaultRepository(
                address = "https://cdn.kde.org/android/stable-releases/fdroid/repo",
                name = "KDE Stables",
                description = "This repository contains unofficial builds of open source apps" +
                    " that are not included in the other repositories.",
                fingerprint = "13784ba6c80ff4e2181e55c56f961eed5844cea16870d3b38d58780b85e1158f"
            ),
            defaultRepository(
                address = "https://zimbelstern.eu/fdroid/repo",
                name = "Zimbelstern's F-Droid repository",
                description = "This is the official repository of apps from zimbelstern.eu," +
                    " to be used with F-Droid.",
                fingerprint = "285158DECEF37CB8DE7C5AF14818ACBF4A9B1FBE63116758EFC267F971CA23AA"
            ),
            defaultRepository(
                address = "https://app.simplex.chat/fdroid/repo",
                name = "SimpleX Chat",
                description = "SimpleX Chat official F-Droid repository.",
                fingerprint = "9F358FF284D1F71656A2BFAF0E005DEAE6AA14143720E089F11FF2DDCFEB01BA"
            ),
            defaultRepository(
                address = "https://f-droid.monerujo.io/fdroid/repo",
                name = "Monerujo Wallet",
                description = "Monerujo Monero Wallet official F-Droid repository.",
                fingerprint = "A82C68E14AF0AA6A2EC20E6B272EFF25E5A038F3F65884316E0F5E0D91E7B713"
            ),
            defaultRepository(
                address = "https://fdroid.cakelabs.com/fdroid/repo",
                name = "Cake Labs",
                description = "Cake Labs official F-Droid repository for Cake Wallet and Monero.com.",
                fingerprint = "EA44EFAEE0B641EE7A032D397D5D976F9C4E5E1ED26E11C75702D064E55F8755"
            ),
            defaultRepository(
                address = "https://app.futo.org/fdroid/repo",
                name = "FUTO",
                description = "FUTO official F-Droid repository.",
                fingerprint = "39D47869D29CBFCE4691D9F7E6946A7B6D7E6FF4883497E6E675744ECDFA6D6D"
            ),
            defaultRepository(
                address = "https://fdroid.mm20.de/repo",
                name = "MM20 Apps",
                description = "Apps developed and distributed by MM20.",
                fingerprint = "156FBAB952F6996415F198F3F29628D24B30E725B0F07A2B49C3A9B5161EEE1A"
            ),
            defaultRepository(
                address = "https://breezy-weather.github.io/fdroid-repo/fdroid/repo",
                name = "Breezy Weather",
                description = "The F-Droid repository for Breezy Weather.",
                fingerprint = "3480A7BB2A296D8F98CB90D2309199B5B9519C1B31978DBCD877ADB102AF35EE"
            ),
            defaultRepository(
                address = "https://gh.artemchep.com/keyguard-repo-fdroid/repo",
                name = "Keyguard Project",
                description = "Mirrors artifacts available on https://github.com/AChep/keyguard-app/releases.",
                fingerprint = "03941CE79B081666609C8A48AB6E46774263F6FC0BBF1FA046CCFFC60EA643BC"
            ),
            defaultRepository(
                address = "https://f5a.torus.icu/fdroid/repo",
                name = "Fcitx5 for Android F-Droid Repo",
                description = "Out-of-tree fcitx5-android plugins.",
                fingerprint = "5D87CE1FAD3772425C2A7ED987A57595A20B07543B9595A7FD2CED25DFF3CF12"
            ),
            defaultRepository(
                address = "https://fdroid.i2pd.xyz/fdroid/repo/",
                name = "PurpleI2P F-Droid repository",
                description = "This is a repository of PurpleI2P. It contains applications developed and supported by our team.",
                fingerprint = "2B9564B0895EEAC039E854C6B065291B01E6A9CA02939CEDD0D35CF44BEE78E0"
            ),
        )

        val newlyAdded: List<Repository> = listOf(
            defaultRepository(
                address = "https://fdroid.ironfoxoss.org/fdroid/repo",
                name = "IronFox",
                description = "The official repository for IronFox:" +
                    " A privacy and security-oriented Firefox-based browser for Android.",
                fingerprint = "C5E291B5A571F9C8CD9A9799C2C94E02EC9703948893F2CA756D67B94204F904"
            ),
            defaultRepository(
                address = "https://raw.githubusercontent.com/chrisgch/tca/master/fdroid/repo",
                name = "Total Commander",
                description = "The official repository for Total Commander",
                fingerprint = "3576596CECDD70488D61CFD90799A49B7FFD26A81A8FEF1BADEC88D069FA72C1"
            ),
            defaultRepository(
                address = "https://www.cromite.org/fdroid/repo",
                name = "Cromite",
                description = "The official repository for Cromite. " +
                    "Cromite is a Chromium fork based on Bromite with " +
                    "built-in support for ad blocking and an eye for privacy.",
                fingerprint = "49F37E74DEE483DCA2B991334FB5A0200787430D0B5F9A783DD5F13695E9517B"
            ),
            defaultRepository(
                address = "https://brave-browser-apk-release.s3.brave.com/fdroid/repo",
                name = "Brave Browser",
                description = "The official repository for Brave Stable releases.",
                fingerprint = "3C60DE135AA19EC949E998469C908F7171885C1E2805F39EB403DDB0F37B4BD2"
            ),
            defaultRepository(
                address = "https://brave-browser-apk-beta.s3.brave.com/fdroid/repo",
                name = "Brave Browser Beta",
                description = "The official repository for Brave Beta releases. ",
                fingerprint = "C4675F87E60D58CECBECD884E43C4A089F2A3FDF58CAF823609B809AC1F90FAE"
            ),
            defaultRepository(
                address = "https://brave-browser-apk-nightly.s3.brave.com/fdroid/repo",
                name = "Brave Browser Nightly",
                description = "The official repository for Brave Nightly releases. ",
                fingerprint = "1735E2323FDE104A74F16D31C062DF75C30E79A706292FABEA1F94E6AF06EE62"
            )
        )

        val toRemove: List<String> = listOf(
            // Add repository addresses that should be removed during database upgrades and remove them from the lists above
            // Example: "https://example.com/fdroid/repo"
            "https://secfirst.org/fdroid/repo",
            "https://fdroid.libretro.com/repo",
            "https://divestos.org/apks/unofficial/fdroid/repo",
            "https://divestos.org/fdroid/official"
        )
    }
}
