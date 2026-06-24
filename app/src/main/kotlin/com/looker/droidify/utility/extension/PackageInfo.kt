package com.looker.droidify.utility.extension

import android.content.pm.PackageInfo
import com.looker.droidify.model.InstalledItem
import com.looker.droidify.utility.common.extension.calculateHash
import com.looker.droidify.utility.common.extension.singleSignature
import com.looker.droidify.utility.common.extension.versionCodeCompat

fun PackageInfo.toInstalledItem(): InstalledItem {
    val signatureString = singleSignature?.calculateHash().orEmpty()
    return InstalledItem(
        packageName,
        versionName.orEmpty(),
        versionCodeCompat,
        signatureString,
    )
}
