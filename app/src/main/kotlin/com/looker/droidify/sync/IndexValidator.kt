package com.looker.droidify.sync

import com.looker.droidify.data.model.Fingerprint
import com.looker.droidify.network.validation.ValidationException
import java.util.jar.JarEntry

interface IndexValidator {

    @Throws(ValidationException::class)
    suspend fun validate(
        jarEntry: JarEntry,
        expectedFingerprint: Fingerprint?,
    ): Fingerprint

}
