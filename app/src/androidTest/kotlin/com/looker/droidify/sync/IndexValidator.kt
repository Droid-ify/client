package com.looker.droidify.sync

import com.looker.droidify.data.model.Fingerprint
import java.util.jar.JarEntry

val FakeIndexValidator = object : IndexValidator {
    override suspend fun validate(
        jarEntry: JarEntry,
        expectedFingerprint: Fingerprint?
    ): Fingerprint {
        return expectedFingerprint ?: Fingerprint("0".repeat(64))
    }
}
