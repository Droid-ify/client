package com.looker.droidify.compose.repoEdit

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.droidify.data.RepoRepository
import com.looker.droidify.network.NetworkResponse
import com.looker.droidify.network.authentication
import com.looker.droidify.network.head
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@HiltViewModel
class RepoEditViewModel @Inject constructor(
    private val repoRepository: RepoRepository,
    private val httpClient: OkHttpClient,
) : ViewModel() {

    val addressState = TextFieldState("")
    val fingerprintState = TextFieldState("")
    val usernameState = TextFieldState("")
    val passwordState = TextFieldState("")

    private val _repoId = MutableStateFlow<Int?>(null)
    val repoId: StateFlow<Int?> = _repoId

    private val _syncError = MutableStateFlow<RepoEditErrorState?>(null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val takenAddresses: StateFlow<Set<String>> = repoRepository.addresses.map {
        it.map { address -> stripPathSuffix(address) }.toSet()
    }.asStateFlow(emptySet())

    private val addressFlow = snapshotFlow { addressState.text.toString() }
    private val fingerprintFlow = snapshotFlow { fingerprintState.text.toString() }
    private val usernameFlow = snapshotFlow { usernameState.text.toString() }
    private val passwordFlow = snapshotFlow { passwordState.text.toString() }

    val errorState = combine(
        addressFlow,
        fingerprintFlow,
        usernameFlow,
        passwordFlow,
        _syncError,
    ) { address, fingerprint, username, password, syncError ->
        RepoEditErrorState(
            addressError = addressError(address) ?: syncError?.addressError,
            fingerprintError = fingerprintError(fingerprint) ?: syncError?.fingerprintError,
            usernameError = usernameError(username, password) ?: syncError?.usernameError,
            passwordError = passwordError(username, password) ?: syncError?.passwordError,
        )
    }.asStateFlow(RepoEditErrorState())

    private val addressSuffixes = arrayOf("fdroid/repo", "repo")

    fun loadRepo(repoId: Int) {
        viewModelScope.launch {
            _repoId.value = repoId
            val repo = repoRepository.getRepo(repoId)
            repo?.let {
                addressState.edit { this.append(it.address) }
                it.fingerprint?.let { fingerprint ->
                    fingerprintState.edit { this.append(formatFingerprint(fingerprint.value)) }
                }
                it.authentication?.let { auth ->
                    usernameState.edit { this.append(auth.username) }
                    passwordState.edit { this.append(auth.password) }
                }
            }
        }
    }

    fun saveRepository(skipCheck: Boolean = false) {
        if (_isLoading.value) return

        val address = addressState.text.toString()
        val fingerprint = fingerprintState.text.toString().replace(" ", "")
        val username = usernameState.text.toString().takeIf { it.isNotEmpty() }
        val password = passwordState.text.toString().takeIf { it.isNotEmpty() }

        if (skipCheck) {
            saveRepositoryToDatabase(address, fingerprint, username, password)
        } else {
            checkAndSaveRepository(address, fingerprint, username, password)
        }
    }

    private fun checkAndSaveRepository(
        address: String,
        fingerprint: String,
        username: String?,
        password: String?,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resultAddress = checkAddress(address, username, password)
                if (resultAddress != null) {
                    saveRepositoryToDatabase(resultAddress, fingerprint, username, password)
                } else {
                    _syncError.value = _syncError.value.copy(addressError = "Repository not found")
                }
            } catch (e: Exception) {
                _syncError.value =
                    _syncError.value.copy(addressError = "Error checking repository: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun checkAddress(
        rawAddress: String,
        username: String?,
        password: String?,
    ): String? = withContext(Dispatchers.IO) {
        val allAddresses = addressSuffixes.map { "$rawAddress/$it" } + rawAddress

        allAddresses
            .sortedBy { it.length }
            .forEach { address ->
                val response = httpClient.head(
                    url = "$address/index-v1.jar",
                    block = {
                        if (username != null && password != null) {
                            authentication(username, password)
                        }
                    },
                )
                if (response is NetworkResponse.Success) return@withContext address
            }
        null
    }

    private fun saveRepositoryToDatabase(
        address: String,
        fingerprint: String,
        username: String?,
        password: String?,
    ) {
        viewModelScope.launch {
            repoRepository.insertRepo(
                address = address,
                fingerprint = fingerprint.ifEmpty { null },
                username = username,
                password = password,
            )
        }
    }

    private fun addressError(address: String): String? {
        val normalizedAddress = normalizeAddress(address)
        return when {
            normalizedAddress == null -> "Invalid address"
            stripPathSuffix(normalizedAddress) in takenAddresses.value -> "Address already exists"
            else -> null
        }
    }

    private fun fingerprintError(fingerprint: String): String? {
        val fin = fingerprint.replace(" ", "")
        return if (fin.isNotEmpty() && fin.length != 64) {
            "Invalid fingerprint format"
        } else {
            null
        }
    }

    private fun usernameError(username: String, password: String): String? = when {
        username.contains(':') -> "Username cannot contain ':'"
        username.isEmpty() && password.isNotEmpty() -> "Username is required"
        else -> null
    }

    private fun passwordError(username: String, password: String): String? = when {
        username.isNotEmpty() && password.isEmpty() -> "Password is required"
        else -> null
    }

    private fun normalizeAddress(address: String): String? {
        val uri = try {
            val uri = URI(address)
            if (uri.isAbsolute) uri.normalize() else null
        } catch (_: URISyntaxException) {
            return null
        }
        return try {
            uri?.toURL()?.toURI()?.toString()?.removeSuffix("/")
        } catch (_: URISyntaxException) {
            null
        }
    }

    private fun formatFingerprint(fingerprint: String): String {
        return fingerprint.uppercase()
            .windowed(2, 2, true)
            .take(32)
            .joinToString(separator = " ")
    }

    private fun stripPathSuffix(address: String): String {
        val cropped = address.removeSuffix("/")
        val endsWith = addressSuffixes
            .sortedByDescending { it.length }
            .find { cropped.endsWith("/$it") }
        return if (endsWith != null) {
            cropped.substring(
                0,
                cropped.length - endsWith.length - 1,
            )
        } else {
            cropped
        }
    }
}

class RepoEditErrorState(
    val addressError: String? = null,
    val fingerprintError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
) {
    val hasError: Boolean =
        (addressError != null) || (fingerprintError != null) || (usernameError != null) || (passwordError != null)

}

private fun RepoEditErrorState?.copy(
    addressError: String? = null,
    fingerprintError: String? = null,
    usernameError: String? = null,
    passwordError: String? = null,
) = RepoEditErrorState(
    addressError = addressError ?: this?.addressError,
    fingerprintError = fingerprintError ?: this?.fingerprintError,
    usernameError = usernameError ?: this?.usernameError,
    passwordError = passwordError ?: this?.passwordError,
)

