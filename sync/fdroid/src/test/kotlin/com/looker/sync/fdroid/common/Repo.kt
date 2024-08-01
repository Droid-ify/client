package com.looker.sync.fdroid.common

import com.looker.core.domain.model.Authentication
import com.looker.core.domain.model.Fingerprint
import com.looker.core.domain.model.Repo
import com.looker.core.domain.model.VersionInfo
import kotlin.math.truncate

val Izzy = Repo(
    id = 1L,
    enabled = true,
    address = "https://apt.izzysoft.de/fdroid/repo",
    name = "IzzyOnDroid F-Droid Repo",
    description = "This is a repository of apps to be used with F-Droid. Applications in this repository are official binaries built by the original application developers, taken from their resp. repositories (mostly Github, GitLab, Codeberg). Updates for the apps are usually fetched daily, and you can expect daily index updates.",
    fingerprint = Fingerprint("0".repeat(64)),
    authentication = Authentication("", ""),
    versionInfo = VersionInfo(0L, null),
    mirrors = emptyList(),
    antiFeatures = emptyList(),
    categories = emptyList(),
)
