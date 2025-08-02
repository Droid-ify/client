package com.looker.droidify.compose.repoEdit

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.droidify.data.RepoRepository
import com.looker.droidify.data.model.Authentication
import com.looker.droidify.network.Downloader
import com.looker.droidify.network.NetworkResponse
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class RepoEditViewModel @Inject constructor(
    private val repoRepository: RepoRepository,
    private val downloader: Downloader,
) : ViewModel() {

    val addressState = TextFieldState("")
    val fingerprintState = TextFieldState("")
    val usernameState = TextFieldState("")
    val passwordState = TextFieldState("")

    private val _repoId = MutableStateFlow<Int?>(null)
    val repoId: StateFlow<Int?> = _repoId

    private val _addressError = MutableStateFlow<String?>(null)
    val addressError: StateFlow<String?> = _addressError

    private val _fingerprintError = MutableStateFlow<String?>(null)
    val fingerprintError: StateFlow<String?> = _fingerprintError

    private val _usernamePasswordError = MutableStateFlow<String?>(null)
    val usernamePasswordError: StateFlow<String?> = _usernamePasswordError

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _takenAddresses = MutableStateFlow<Set<String>>(emptySet())
    private val takenAddresses: StateFlow<Set<String>> = _takenAddresses

    private val addressFlow = snapshotFlow { addressState.text.toString() }
    private val fingerprintFlow = snapshotFlow { fingerprintState.text.toString() }
    private val usernameFlow = snapshotFlow { usernameState.text.toString() }
    private val passwordFlow = snapshotFlow { passwordState.text.toString() }

    val isFormValid = combine(
        addressError,
        fingerprintError,
        usernamePasswordError,
        isLoading
    ) { addressError, fingerprintError, usernamePasswordError, isLoading ->
        addressError == null && fingerprintError == null && usernamePasswordError == null && !isLoading
    }.asStateFlow(false)

    private val addressSuffixes = arrayOf("fdroid/repo", "repo")

    init {
        viewModelScope.launch {
            val repos = repoRepository.repos.first()
            _takenAddresses.value = repos
                .filter { it.id != _repoId.value }
                .flatMap { listOf(it.address) + it.mirrors }
                .map { it.withoutKnownPath }
                .toSet()
        }

        viewModelScope.launch {
            addressFlow.collect { validateAddress() }
        }

        viewModelScope.launch {
            fingerprintFlow.collect { validateFingerprint() }
        }

        viewModelScope.launch {
            combine(usernameFlow, passwordFlow) { username, password ->
                Pair(username, password)
            }.collect { (username, password) ->
                validateUsernamePassword(username, password)
            }
        }
    }

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

        val address = normalizeAddress(addressState.text.toString()) ?: run {
            _addressError.value = "Invalid address"
            return
        }
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
        password: String?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resultAddress = checkAddress(address, username, password)
                if (resultAddress != null) {
                    saveRepositoryToDatabase(resultAddress, fingerprint, username, password)
                } else {
                    _addressError.value = "Repository unreachable"
                }
            } catch (e: Exception) {
                _addressError.value = "Error checking repository: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun checkAddress(
        rawAddress: String,
        username: String?,
        password: String?
    ): String? = withContext(Dispatchers.IO) {
        val allAddresses = addressSuffixes.map { "$rawAddress/$it" } + rawAddress
        val authentication = if (username != null && password != null) {
            Authentication(username, password)
        } else {
            null
        }

        allAddresses
            .sortedBy { it.length }
            .forEach { address ->
                val response = downloader.headCall(
                    url = "$address/index-v1.jar",
                    headers = {
                        authentication?.let {
                            authentication("Basic ${encodeCredentials(it.username, it.password)}")
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
        password: String?
    ) {
        viewModelScope.launch {
            try {
                repoRepository.insertRepo(
                    address = address,
                    fingerprint = if (fingerprint.isNotEmpty()) fingerprint else null,
                    username = username,
                    password = password
                )
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun validateAddress() {
        val addressText = addressState.text.toString()
        val normalizedAddress = normalizeAddress(addressText)

        _addressError.value = when {
            normalizedAddress == null -> "Invalid address"
            normalizedAddress.withoutKnownPath in takenAddresses.value -> "Address already exists"
            else -> null
        }
    }

    private fun validateFingerprint() {
        val fingerprint = fingerprintState.text.toString().replace(" ", "")
        _fingerprintError.value = if (fingerprint.isNotEmpty() && fingerprint.length != 64) {
            "Invalid fingerprint format"
        } else {
            null
        }
    }

    private fun validateUsernamePassword(username: String, password: String) {
        _usernamePasswordError.value = when {
            username.contains(':') -> "Username cannot contain ':'"
            username.isEmpty() && password.isNotEmpty() -> "Username is required"
            username.isNotEmpty() && password.isEmpty() -> "Password is required"
            else -> null
        }
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

    private fun encodeCredentials(username: String, password: String): String {
        val credentials = "$username:$password"
        return android.util.Base64.encodeToString(
            credentials.toByteArray(),
            android.util.Base64.NO_WRAP
        )
    }

    private val String.pathCropped: String
        get() {
            val index = indexOfLast { it != '/' }
            return if (index >= 0 && index < length - 1) substring(0, index + 1) else this
        }

    private val String.withoutKnownPath: String
        get() {
            val cropped = pathCropped
            val endsWith =
                addressSuffixes.asSequence()
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
