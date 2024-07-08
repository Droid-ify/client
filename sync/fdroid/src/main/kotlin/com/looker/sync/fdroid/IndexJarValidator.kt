package com.looker.sync.fdroid

import com.looker.core.common.extension.certificate
import com.looker.core.common.extension.codeSigner
import com.looker.core.common.extension.fingerprint
import com.looker.core.common.signature.ValidationException
import com.looker.core.domain.model.Fingerprint
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
            Fingerprint(
                value = jarEntry
                    .codeSigner
                    .certificate
                    .fingerprint()
            )
        } catch (e: IllegalStateException) {
            throw ValidationException(e.message ?: "Unknown Exception")
        } catch (e: IllegalArgumentException) {
            throw ValidationException(e.message ?: "Error creating Fingerprint object")
        }
        if (expectedFingerprint == null) {
            fingerprint
        } else {
            if (expectedFingerprint.check(fingerprint)) {
                expectedFingerprint
            } else {
                throw ValidationException(
                    "Expected Fingerprint: ${expectedFingerprint}, " +
                        "Acquired Fingerprint: $fingerprint"
                )
            }
        }
    }
}
