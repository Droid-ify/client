package com.looker.droidify.service

import android.content.Context
import androidx.annotation.StringRes
import com.looker.droidify.data.encryption.sha256
import com.looker.droidify.data.model.hex
import com.looker.droidify.model.Release
import com.looker.droidify.network.validation.FileValidator
import com.looker.droidify.network.validation.ValidationResult
import com.looker.droidify.utility.common.extension.calculateHash
import com.looker.droidify.utility.common.extension.getPackageArchiveInfoCompat
import com.looker.droidify.utility.common.extension.singleSignature
import com.looker.droidify.utility.common.extension.versionCodeCompat
import java.io.File
import com.looker.droidify.R.string as strings

class ReleaseFileValidator(
    private val context: Context,
    private val packageName: String,
    private val release: Release,
) : FileValidator {

    override suspend fun validate(file: File): ValidationResult {
        val checksum = sha256(file).hex()
        if (!checksum.equals(release.hash, ignoreCase = true)) {
            return invalid(strings.integrity_check_error_DESC)
        }
        val packageInfo = context.packageManager.getPackageArchiveInfoCompat(file.path)
            ?: return invalid(strings.file_format_error_DESC)
        if (packageInfo.packageName != packageName ||
            packageInfo.versionCodeCompat != release.versionCode
        ) {
            return invalid(strings.invalid_metadata_error_DESC)
        }

        packageInfo.singleSignature
            ?.calculateHash()
            ?.takeIf { it.isNotBlank() || it == release.signature }
            ?: return invalid(strings.invalid_signature_error_DESC)

        val permissions = packageInfo.permissions
            ?.map { it.name }
            ?.toSet()
            .orEmpty()
        if (!release.permissions.containsAll(permissions)) {
            return invalid(strings.invalid_permissions_error_DESC)
        }
        return ValidationResult.Valid
    }

    private fun invalid(@StringRes id: Int): ValidationResult.Invalid =
        ValidationResult.Invalid(context.getString(id))
}
