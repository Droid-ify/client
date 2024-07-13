package com.looker.droidify.utility.extension

import android.content.pm.PackageInfo
import com.looker.core.common.extension.calculateHash
import com.looker.core.common.extension.singleSignature
import com.looker.core.common.extension.versionCodeCompat
import com.looker.droidify.model.InstalledItem

fun PackageInfo.toInstalledItem(): InstalledItem {
    val signatureString = singleSignature?.calculateHash().orEmpty()
    return InstalledItem(
        packageName,
        versionName.orEmpty(),
        versionCodeCompat,
        signatureString
    )
}
