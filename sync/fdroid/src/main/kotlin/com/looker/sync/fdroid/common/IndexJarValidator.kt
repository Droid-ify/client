package com.looker.sync.fdroid.common

import com.looker.core.domain.model.Fingerprint
import com.looker.core.domain.model.fingerprint
import com.looker.network.validation.invalid
import com.looker.sync.fdroid.IndexValidator
import com.looker.sync.fdroid.utils.certificate
import com.looker.sync.fdroid.utils.codeSigner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.jar.JarEntry

class IndexJarValidator(
    private val dispatcher: CoroutineDispatcher
) : IndexValidator {
    override suspend fun validate(
        jarEntry: JarEntry,
        expectedFingerprint: Fingerprint?
    ): Fingerprint = withContext(dispatcher) {
        val fingerprint = try {
            jarEntry
                .codeSigner
                .certificate
                .fingerprint()
        } catch (e: IllegalStateException) {
            invalid(e.message ?: "Unknown Exception")
        } catch (e: IllegalArgumentException) {
            invalid(e.message ?: "Error creating Fingerprint object")
        }
        if (expectedFingerprint == null) {
            fingerprint
        } else {
            if (expectedFingerprint.check(fingerprint)) {
                expectedFingerprint
            } else {
                invalid(
                    "Expected Fingerprint: ${expectedFingerprint}, " +
                        "Acquired Fingerprint: $fingerprint"
                )
            }
        }
    }
}
