package com.looker.sync.fdroid

import com.looker.core.common.signature.ValidationException
import com.looker.core.domain.model.Fingerprint
import java.util.jar.JarEntry

interface IndexValidator {

    @Throws(ValidationException::class)
    suspend fun validate(
        jarEntry: JarEntry,
        expectedFingerprint: Fingerprint?,
    ): Fingerprint

}
