package com.looker.sync.fdroid

import com.looker.core.domain.model.Fingerprint
import com.looker.network.validation.ValidationException
import java.util.jar.JarEntry

interface IndexValidator {

    @Throws(ValidationException::class)
    suspend fun validate(
        jarEntry: JarEntry,
        expectedFingerprint: Fingerprint?,
    ): Fingerprint

}
