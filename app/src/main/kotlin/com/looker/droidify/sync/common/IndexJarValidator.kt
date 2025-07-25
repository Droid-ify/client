package com.looker.droidify.sync.common

import com.looker.droidify.domain.model.Fingerprint
import com.looker.droidify.domain.model.check
import com.looker.droidify.domain.model.fingerprint
import com.looker.droidify.network.validation.invalid
import com.looker.droidify.sync.IndexValidator
import com.looker.droidify.sync.utils.certificate
import com.looker.droidify.sync.utils.codeSigner
import java.util.jar.JarEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

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
        if (!fingerprint.isValid) invalid("Invalid Fingerprint: $fingerprint")
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
