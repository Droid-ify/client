package com.looker.droidify.service

import android.content.Context
import androidx.annotation.StringRes
import com.looker.core.common.extension.calculateHash
import com.looker.core.common.extension.getPackageArchiveInfoCompat
import com.looker.core.common.extension.singleSignature
import com.looker.core.common.extension.versionCodeCompat
import com.looker.network.validation.FileValidator
import com.looker.core.common.signature.Hash
import com.looker.network.validation.invalid
import com.looker.core.common.signature.verifyHash
import com.looker.droidify.model.Release
import java.io.File
import com.looker.core.common.R.string as strings

class ReleaseFileValidator(
    private val context: Context,
    private val packageName: String,
    private val release: Release
) : FileValidator {

    override suspend fun validate(file: File) {
        val hash = Hash(release.hashType, release.hash)
        if (!file.verifyHash(hash)) {
            invalid(getString(strings.integrity_check_error_DESC))
        }
        val packageInfo = context.packageManager.getPackageArchiveInfoCompat(file.path)
            ?: invalid(getString(strings.file_format_error_DESC))
        if (packageInfo.packageName != packageName ||
            packageInfo.versionCodeCompat != release.versionCode
        ) {
            invalid(getString(strings.invalid_metadata_error_DESC))
        }

        packageInfo.singleSignature
            ?.calculateHash()
            ?.takeIf { it.isNotBlank() || it == release.signature }
            ?: invalid(getString(strings.invalid_signature_error_DESC))

        packageInfo.permissions
            ?.asSequence()
            .orEmpty()
            .map { it.name }
            .toSet()
            .takeIf { release.permissions.containsAll(it) }
            ?: invalid(getString(strings.invalid_permissions_error_DESC))
    }

    private fun getString(@StringRes id: Int): String = context.getString(id)
}
