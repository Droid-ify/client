package com.looker.droidify.sync.common

import com.looker.droidify.data.model.Authentication
import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.data.model.Html
import com.looker.droidify.data.model.Repo
import com.looker.droidify.data.model.VersionInfo

val Izzy = Repo(
    id = 1,
    enabled = true,
    address = "https://apt.izzysoft.de/fdroid/repo",
    name = "IzzyOnDroid F-Droid Repo",
    description = Html("This is a repository of apps to be used with F-Droid. Applications in this repository are official binaries built by the original application developers, taken from their resp. repositories (mostly Github, GitLab, Codeberg). Updates for the apps are usually fetched daily, and you can expect daily index updates."),
    fingerprint = Fingerprint("3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A"),
    authentication = Authentication("", ""),
    versionInfo = VersionInfo(0L, null),
    mirrors = emptyList(),
    icon = null,
)
