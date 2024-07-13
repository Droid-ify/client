package com.looker.droidify.ui.repository

import android.os.Bundle
import android.text.Selection
import android.util.Base64
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.looker.core.common.extension.clipboardManager
import com.looker.core.common.extension.get
import com.looker.core.common.extension.getMutatedIcon
import com.looker.core.common.nullIfEmpty
import com.looker.droidify.model.Repository
import com.looker.droidify.database.Database
import com.looker.droidify.databinding.EditRepositoryBinding
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.ui.Message
import com.looker.droidify.ui.MessageDialog
import com.looker.droidify.ui.ScreenFragment
import com.looker.droidify.utility.extension.screenActivity
import com.looker.network.Downloader
import com.looker.network.NetworkResponse
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.Charset
import java.util.Locale
import javax.inject.Inject
import kotlin.math.min
import com.looker.core.common.R as CommonR
import com.looker.core.common.R.string as stringRes

@AndroidEntryPoint
class EditRepositoryFragment() : ScreenFragment() {

    constructor(repositoryId: Long?, repoAddress: String?) : this() {
        arguments =
            bundleOf(EXTRA_REPOSITORY_ID to repositoryId, EXTRA_REPOSITORY_ADDRESS to repoAddress)
    }

    private var _binding: EditRepositoryBinding? = null
    private val binding get() = _binding!!

    private val repoId: Long?
        get() = arguments?.getLong(EXTRA_REPOSITORY_ID)

    private val repoAddress: String?
        get() = arguments?.getString(EXTRA_REPOSITORY_ADDRESS)

    private var saveMenuItem: MenuItem? = null

    private val syncConnection = Connection(SyncService::class.java)
    private var checkInProgress = false
    private var checkJob: Job? = null

    private var takenAddresses = emptySet<String>()

    @Inject
    lateinit var downloader: Downloader

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = EditRepositoryBinding.inflate(layoutInflater)

        syncConnection.bind(requireContext())

        screenActivity.onToolbarCreated(toolbar)
        toolbar.title =
            getString(
                if (repoId != null) stringRes.edit_repository else stringRes.add_repository
            )

        saveMenuItem = toolbar.menu.add(stringRes.save)
            .setIcon(toolbar.context.getMutatedIcon(CommonR.drawable.ic_save))
            .setEnabled(false)
            .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS).setOnMenuItemClickListener {
                onSaveRepositoryClick(true)
                true
            }

        val content = fragmentBinding.fragmentContent

        content.addView(binding.root)

        val validChar: (Char) -> Boolean = { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

        binding.fingerprint.doAfterTextChanged { text ->
            fun logicalPosition(text: String, position: Int): Int {
                return if (position > 0) {
                    text.asSequence().take(position)
                        .count(validChar)
                } else {
                    position
                }
            }

            fun realPosition(text: String, position: Int): Int {
                return if (position > 0) {
                    var left = position
                    val index = text.indexOfFirst {
                        validChar(it) && run {
                            left -= 1
                            left <= 0
                        }
                    }
                    if (index >= 0) min(index + 1, text.length) else text.length
                } else {
                    position
                }
            }

            val inputString = text.toString()
            val outputString = inputString
                .uppercase(Locale.US)
                .filter(validChar)
                .windowed(2, 2, true).take(32)
                .joinToString(separator = " ")
            if (inputString != outputString) {
                val inputStart = logicalPosition(inputString, Selection.getSelectionStart(text))
                val inputEnd = logicalPosition(inputString, Selection.getSelectionEnd(text))
                text?.replace(0, text.length, outputString)
                Selection.setSelection(
                    text,
                    realPosition(outputString, inputStart),
                    realPosition(outputString, inputEnd)
                )
            }
        }

        if (savedInstanceState == null) {
            val repository = repoId?.let(Database.RepositoryAdapter::get)
            if (repository == null) {
                val text = repoAddress ?: kotlin.run {
                    context?.clipboardManager?.primaryClip?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)?.text?.toString().orEmpty()
                }
                val (addressText, fingerprintText) = try {
                    val uri = URL(text).toString().toUri()
                    val fingerprintText = uri["fingerprint"]?.nullIfEmpty()
                        ?: uri["FINGERPRINT"]?.nullIfEmpty()
                    Pair(
                        uri.buildUpon().path(uri.path?.pathCropped).query(null).fragment(null)
                            .build().toString(),
                        fingerprintText
                    )
                } catch (e: Exception) {
                    Pair(null, null)
                }
                binding.address.setText(addressText)
                binding.fingerprint.setText(fingerprintText)
            } else {
                binding.address.setText(repository.address)
                val mirrors = repository.mirrors.map { it.withoutKnownPath }
                binding.addressContainer.apply {
                    isEndIconVisible = mirrors.isNotEmpty()
                    setEndIconDrawable(CommonR.drawable.ic_arrow_down)
                    setEndIconOnClickListener {
                        SelectMirrorDialog(mirrors).show(
                            childFragmentManager,
                            SelectMirrorDialog::class.java.name
                        )
                    }
                }
                binding.fingerprint.setText(repository.fingerprint)
                val (usernameText, passwordText) = repository.authentication.nullIfEmpty()
                    ?.let { if (it.startsWith("Basic ")) it.substring(6) else null }?.let {
                        try {
                            Base64.decode(it, Base64.NO_WRAP).toString(Charset.defaultCharset())
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }?.let {
                        val index = it.indexOf(':')
                        if (index >= 0) {
                            Pair(
                                it.substring(0, index),
                                it.substring(index + 1)
                            )
                        } else {
                            null
                        }
                    } ?: Pair(null, null)
                binding.username.setText(usernameText)
                binding.password.setText(passwordText)
            }
        }

        binding.address.doAfterTextChanged { invalidateAddress() }
        binding.fingerprint.doAfterTextChanged { invalidateFingerprint() }
        binding.username.doAfterTextChanged { invalidateUsernamePassword() }
        binding.password.doAfterTextChanged { invalidateUsernamePassword() }

        (binding.overlay.parent as ViewGroup).layoutTransition?.setDuration(200L)
        binding.overlay.background!!.apply {
            mutate()
            alpha = 0xcc
        }
        binding.skip.setOnClickListener {
            if (checkInProgress) {
                checkInProgress = false
                checkJob?.cancel()
                onSaveRepositoryClick(false)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val list = Database.RepositoryAdapter.getAll()
            takenAddresses = list.asSequence().filter { it.id != repoId }
                .flatMap { (it.mirrors + it.address).asSequence() }
                .map { it.withoutKnownPath }
                .toSet()
            invalidateAddress()
        }
        invalidateAddress()
        invalidateFingerprint()
        invalidateUsernamePassword()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        saveMenuItem = null
        syncConnection.unbind(requireContext())
        _binding = null
    }

    private var addressError = false
    private var fingerprintError = false
    private var usernamePasswordError = false

    private fun invalidateAddress() {
        invalidateAddress(binding.address.text.toString())
    }

    private fun invalidateAddress(addressText: String) {
        val normalizedAddress = normalizeAddress(addressText)
        val addressErrorResId = if (normalizedAddress != null) {
            if (normalizedAddress.withoutKnownPath in takenAddresses) {
                stringRes.already_exists
            } else {
                null
            }
        } else {
            stringRes.invalid_address
        }
        addressError = addressErrorResId != null
        addressErrorResId?.let { binding.address.error = getString(it) }
        invalidateState()
    }

    private fun invalidateFingerprint() {
        val fingerprint = binding.fingerprint.text.toString().replace(" ", "")
        val fingerprintInvalid = fingerprint.isNotEmpty() && fingerprint.length != 64
        if (fingerprintInvalid) {
            binding.fingerprint.error = getString(stringRes.invalid_fingerprint_format)
        }
        fingerprintError = fingerprintInvalid
        invalidateState()
    }

    private fun invalidateUsernamePassword() {
        val username = binding.username.text.toString()
        val password = binding.password.text.toString()
        val usernameInvalid = username.contains(':')
        val usernameEmpty = username.isEmpty() && password.isNotEmpty()
        val passwordEmpty = username.isNotEmpty() && password.isEmpty()
        if (usernameEmpty) {
            binding.username.error = getString(stringRes.username_missing)
        } else if (passwordEmpty) {
            binding.password.error = getString(stringRes.password_missing)
        } else if (usernameInvalid) {
            binding.username.error = getString(stringRes.invalid_username_format)
        }
        usernamePasswordError = usernameInvalid || usernameEmpty || passwordEmpty
        invalidateState()
    }

    private fun invalidateState() {
        saveMenuItem!!.isEnabled =
            !addressError && !fingerprintError && !usernamePasswordError && !checkInProgress
        binding.apply {
            sequenceOf(address, fingerprint, username, password).forEach {
                it.isEnabled = !checkInProgress
            }
        }
        binding.overlay.isVisible = checkInProgress
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
                    cropped.length - endsWith.length - 1
                )
            } else {
                cropped
            }
        }

    private fun normalizeAddress(address: String): String? {
        val uri = try {
            val uri = URI(address)
            if (uri.isAbsolute) uri.normalize() else null
        } catch (e: URISyntaxException) {
            return null
        }
        return try {
            uri?.toURL()?.toURI()?.toString()?.removeSuffix("/")
        } catch (e: URISyntaxException) {
            null
        }
    }

    private fun setMirror(address: String) {
        binding.address.setText(address)
    }

    private fun onSaveRepositoryClick(check: Boolean) {
        if (!checkInProgress) {
            val address = normalizeAddress(binding.address.text.toString())!!
            val fingerprint = binding.fingerprint.text.toString().replace(" ", "")
            val username = binding.username.text.toString().nullIfEmpty()
            val password = binding.password.text.toString().nullIfEmpty()
            val authentication = username?.let { u ->
                password?.let { p ->
                    Base64.encodeToString(
                        "$u:$p".toByteArray(Charset.defaultCharset()),
                        Base64.NO_WRAP
                    )
                }
            }?.let { "Basic $it" }.orEmpty()

            if (check) {
                checkJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    val resultAddress = try {
                        checkAddress(address, authentication)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        failedAddressCheck()
                        null
                    }
                    val allow = resultAddress == address || run {
                        if (resultAddress == null) return@run false
                        binding.address.setText(resultAddress)
                        invalidateAddress(resultAddress)
                        !addressError
                    }
                    if (allow && resultAddress != null) {
                        onSaveRepositoryProceedInvalidate(
                            resultAddress,
                            fingerprint,
                            authentication
                        )
                    } else {
                        invalidateState()
                    }
                    invalidateState()
                }
            } else {
                onSaveRepositoryProceedInvalidate(address, fingerprint, authentication)
            }
        }
    }

    private suspend fun checkAddress(
        address: String,
        authentication: String
    ): String? = coroutineScope {
        checkInProgress = true
        invalidateState()
        val allAddresses = addressSuffixes.map { "$address/$it" } + address
        val pathCheck = allAddresses.map {
            async {
                downloader.headCall(
                    url = "$it/index-v1.jar",
                    headers = { authentication(authentication) }
                ) is NetworkResponse.Success
            }
        }
        val indexOfValidAddress = pathCheck.awaitAll().indexOf(true)
        allAddresses[indexOfValidAddress].nullIfEmpty()
    }

    private fun onSaveRepositoryProceedInvalidate(
        address: String,
        fingerprint: String,
        authentication: String
    ) {
        val binder = syncConnection.binder
        if (binder != null) {
            val repositoryId = repoId
            if (repositoryId != null && binder.isCurrentlySyncing(repositoryId)) {
                MessageDialog(Message.CantEditSyncing).show(childFragmentManager)
                invalidateState()
            } else {
                val repository = repositoryId?.let(Database.RepositoryAdapter::get)
                    ?.edit(address, fingerprint, authentication)
                    ?: Repository.newRepository(address, fingerprint, authentication)
                val changedRepository = Database.RepositoryAdapter.put(repository)
                if (repositoryId == null && changedRepository.enabled) {
                    binder.sync(changedRepository)
                }
                screenActivity.onBackPressedDispatcher.onBackPressed()
            }
        } else {
            invalidateState()
        }
    }

    private fun failedAddressCheck() {
        checkInProgress = false
        invalidateState()
        Snackbar.make(
            requireView(),
            CommonR.string.repository_unreachable,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    class SelectMirrorDialog() : DialogFragment() {
        constructor(mirrors: List<String>) : this() {
            arguments = bundleOf(EXTRA_MIRRORS to ArrayList(mirrors))
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
            val mirrors = requireArguments().getStringArrayList(EXTRA_MIRRORS)!!
            return MaterialAlertDialogBuilder(requireContext()).setTitle(stringRes.select_mirror)
                .setItems(mirrors.toTypedArray()) { _, position ->
                    (parentFragment as EditRepositoryFragment).setMirror(mirrors[position])
                }.setNegativeButton(stringRes.cancel, null).create()
        }

        private companion object {
            const val EXTRA_MIRRORS = "mirrors"
        }
    }

    private companion object {
        const val EXTRA_REPOSITORY_ID = "repositoryId"
        const val EXTRA_REPOSITORY_ADDRESS = "repositoryAddress"

        val addressSuffixes = listOf("fdroid/repo", "repo")
    }
}
