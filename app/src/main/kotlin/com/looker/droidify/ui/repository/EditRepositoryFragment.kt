package com.looker.droidify.ui.repository

import android.net.Uri
import android.os.Bundle
import android.text.Selection
import android.util.Base64
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
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
import com.looker.core.model.Repository
import com.looker.droidify.database.Database
import com.looker.droidify.databinding.EditRepositoryBinding
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.ui.MessageDialog
import com.looker.droidify.ui.ScreenFragment
import com.looker.droidify.utility.extension.screenActivity
import com.looker.network.Downloader
import com.looker.network.NetworkResponse
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.net.URI
import java.net.URL
import java.nio.charset.Charset
import java.util.Locale
import javax.inject.Inject
import kotlin.math.min
import com.looker.core.common.R as CommonR
import com.looker.core.common.R.string as stringRes

@AndroidEntryPoint
class EditRepositoryFragment() : ScreenFragment() {

	private var _editRepositoryBinding: EditRepositoryBinding? = null
	private val editRepositoryBinding get() = _editRepositoryBinding!!

	companion object {
		private const val EXTRA_REPOSITORY_ID = "repositoryId"
		private const val EXTRA_REPOSITORY_ADDRESS = "repositoryAddress"

		private val checkPaths = listOf("", "fdroid/repo", "repo")
	}

	constructor(repositoryId: Long?, repoAddress: String?) : this() {
		arguments = Bundle().apply {
			repositoryId?.let { putLong(EXTRA_REPOSITORY_ID, it) }
			repoAddress?.let { putString(EXTRA_REPOSITORY_ADDRESS, it) }
		}
	}

	private class Layout(view: EditRepositoryBinding) {
		val addressContainer = view.addressContainer
		val address = view.address
		val fingerprint = view.fingerprint
		val username = view.username
		val password = view.password
		val overlay = view.overlay
		val skip = view.skip
	}

	private val repositoryId: Long?
		get() = requireArguments().let {
			if (it.containsKey(EXTRA_REPOSITORY_ID)) it.getLong(EXTRA_REPOSITORY_ID) else null
		}

	private val repositoryAddress: String?
		get() = requireArguments().let {
			if (it.containsKey(EXTRA_REPOSITORY_ADDRESS)) it.getString(EXTRA_REPOSITORY_ADDRESS) else null
		}

	private var saveMenuItem: MenuItem? = null
	private var layout: Layout? = null

	private val syncConnection = Connection(SyncService::class.java)
	private var checkInProgress = false
	private var checkJob: Job? = null

	private var takenAddresses = emptySet<String>()

	@Inject
	lateinit var downloader: Downloader

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		_editRepositoryBinding = EditRepositoryBinding.inflate(layoutInflater)

		syncConnection.bind(requireContext())

		screenActivity.onToolbarCreated(toolbar)
		toolbar.title =
			getString(if (repositoryId != null) stringRes.edit_repository else stringRes.add_repository)

		saveMenuItem = toolbar.menu.add(stringRes.save)
			.setIcon(toolbar.context.getMutatedIcon(CommonR.drawable.ic_save))
			.setEnabled(false)
			.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS).setOnMenuItemClickListener {
				onSaveRepositoryClick(true)
				true
			}

		val content = fragmentBinding.fragmentContent

		content.addView(editRepositoryBinding.root)
		val layout = Layout(editRepositoryBinding)
		this.layout = layout

		val validChar: (Char) -> Boolean = { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

		layout.fingerprint.doAfterTextChanged { text ->
			fun logicalPosition(text: String, position: Int): Int {
				return if (position > 0) text.asSequence().take(position)
					.count(validChar) else position
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
			val outputString =
				inputString.uppercase(Locale.US).filter(validChar).windowed(2, 2, true).take(32)
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
			val repository = repositoryId?.let(Database.RepositoryAdapter::get)
			if (repository == null) {
				val text = repositoryAddress ?: kotlin.run {
					context?.clipboardManager?.primaryClip?.takeIf { it.itemCount > 0 }
						?.getItemAt(0)?.text?.toString().orEmpty()
				}
				val (addressText, fingerprintText) = try {
					val uri = URL(text).toString().toUri()
					val fingerprintText = uri["fingerprint"]?.nullIfEmpty()
						?: uri["FINGERPRINT"]?.nullIfEmpty()
					Pair(
						uri.buildUpon().path(uri.path?.pathCropped).query(null).fragment(null)
							.build().toString(), fingerprintText
					)
				} catch (e: Exception) {
					Pair(null, null)
				}
				layout.address.setText(addressText)
				layout.fingerprint.setText(fingerprintText)
			} else {
				layout.address.setText(repository.address)
				val mirrors = repository.mirrors.map { it.withoutKnownPath }
				layout.addressContainer.apply {
					isEndIconVisible = mirrors.isNotEmpty()
					setEndIconDrawable(CommonR.drawable.ic_arrow_down)
					setEndIconOnClickListener {
						SelectMirrorDialog(mirrors).show(
							childFragmentManager,
							SelectMirrorDialog::class.java.name
						)
					}
				}
				layout.fingerprint.setText(repository.fingerprint)
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
						if (index >= 0) Pair(
							it.substring(0, index), it.substring(index + 1)
						) else null
					} ?: Pair(null, null)
				layout.username.setText(usernameText)
				layout.password.setText(passwordText)
			}
		}

		layout.address.doAfterTextChanged { invalidateAddress() }
		layout.fingerprint.doAfterTextChanged { invalidateFingerprint() }
		layout.username.doAfterTextChanged { invalidateUsernamePassword() }
		layout.password.doAfterTextChanged { invalidateUsernamePassword() }

		(layout.overlay.parent as ViewGroup).layoutTransition?.setDuration(200L)
		layout.overlay.background!!.apply {
			mutate()
			alpha = 0xcc
		}
		layout.skip.setOnClickListener {
			if (checkInProgress) {
				checkInProgress = false
				checkJob?.cancel()
				onSaveRepositoryClick(false)
			}
		}

		viewLifecycleOwner.lifecycleScope.launch {
			val list = Database.RepositoryAdapter.getAll()
			takenAddresses = list.asSequence().filter { it.id != repositoryId }
				.flatMap { (it.mirrors + it.address).asSequence() }.map { it.withoutKnownPath }
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
		layout = null

		syncConnection.unbind(requireContext())
		_editRepositoryBinding = null
	}

	private var addressError = false
	private var fingerprintError = false
	private var usernamePasswordError = false

	private fun invalidateAddress() {
		invalidateAddress(layout!!.address.text.toString())
	}

	private fun invalidateAddress(addressText: String) {
		val layout = layout!!
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
		addressErrorResId?.let { layout.address.error = getString(it) }
		invalidateState()
	}

	private fun invalidateFingerprint() {
		val layout = layout!!
		val fingerprint = layout.fingerprint.text.toString().replace(" ", "")
		val fingerprintInvalid = fingerprint.isNotEmpty() && fingerprint.length != 64
		if (fingerprintInvalid) {
			layout.fingerprint.error = getString(stringRes.invalid_fingerprint_format)
		}
		fingerprintError = fingerprintInvalid
		invalidateState()
	}

	private fun invalidateUsernamePassword() {
		val layout = layout!!
		val username = layout.username.text.toString()
		val password = layout.password.text.toString()
		val usernameInvalid = username.contains(':')
		val usernameEmpty = username.isEmpty() && password.isNotEmpty()
		val passwordEmpty = username.isNotEmpty() && password.isEmpty()
		if (usernameEmpty) {
			layout.username.error = getString(stringRes.username_missing)
		} else if (passwordEmpty) {
			layout.password.error = getString(stringRes.password_missing)
		} else if (usernameInvalid) {
			layout.username.error = getString(stringRes.invalid_username_format)
		}
		usernamePasswordError = usernameInvalid || usernameEmpty || passwordEmpty
		invalidateState()
	}

	private fun invalidateState() {
		val layout = layout!!
		saveMenuItem!!.isEnabled =
			!addressError && !fingerprintError && !usernamePasswordError && !checkInProgress
		layout.apply {
			sequenceOf(address, fingerprint, username, password).forEach {
				it.isEnabled = !checkInProgress
			}
		}
		layout.overlay.isVisible = checkInProgress
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
				checkPaths.asSequence().filter { it.isNotEmpty() }.sortedByDescending { it.length }
					.find { cropped.endsWith("/$it") }
			return if (endsWith != null) cropped.substring(
				0, cropped.length - endsWith.length - 1
			) else cropped
		}

	private fun normalizeAddress(address: String): String? {
		val uri = try {
			val uri = URI(address)
			if (uri.isAbsolute) uri.normalize() else null
		} catch (e: Exception) {
			null
		}
		val path = uri?.path?.pathCropped
		return if (uri != null && path != null) {
			try {
				URI(
					uri.scheme, uri.userInfo, uri.host, uri.port, path, uri.query, uri.fragment
				).toString()
			} catch (e: Exception) {
				null
			}
		} else {
			null
		}
	}

	private fun setMirror(address: String) {
		layout?.address?.setText(address)
	}

	private fun onSaveRepositoryClick(check: Boolean) {
		if (!checkInProgress) {
			val layout = layout!!
			val address = normalizeAddress(layout.address.text.toString())!!
			val fingerprint = layout.fingerprint.text.toString().replace(" ", "")
			val username = layout.username.text.toString().nullIfEmpty()
			val password = layout.password.text.toString().nullIfEmpty()
			val authentication = username?.let { u ->
				password?.let { p ->
					Base64.encodeToString(
						"$u:$p".toByteArray(Charset.defaultCharset()), Base64.NO_WRAP
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
						layout.address.setText(resultAddress)
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
	) = coroutineScope {
		checkInProgress = true
		invalidateState()
		val allAddresses = checkPaths.map { path ->
			address + if (path.isEmpty()) "" else "/$path"
		}
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
		authentication: String,
	) {
		val binder = syncConnection.binder
		if (binder != null) {
			val repositoryId = repositoryId
			if (repositoryId != null && binder.isCurrentlySyncing(repositoryId)) {
				MessageDialog(MessageDialog.Message.CantEditSyncing).show(childFragmentManager)
				invalidateState()
			} else {
				val repository = repositoryId?.let(Database.RepositoryAdapter::get)
					?.edit(address, fingerprint, authentication)
					?: Repository.newRepository(address, fingerprint, authentication)
				val changedRepository = Database.RepositoryAdapter.put(repository)
				if (repositoryId == null && changedRepository.enabled) {
					binder.sync(changedRepository)
				}
				screenActivity.onBackPressed()
			}
		} else {
			invalidateState()
		}
	}

	private fun failedAddressCheck() {
		checkInProgress = false
		invalidateState()
		Snackbar.make(
			requireView(), CommonR.string.repository_unreachable, Snackbar.LENGTH_SHORT
		).show()
	}

	class SelectMirrorDialog() : DialogFragment() {
		companion object {
			private const val EXTRA_MIRRORS = "mirrors"
		}

		constructor(mirrors: List<String>) : this() {
			arguments = Bundle().apply {
				putStringArrayList(EXTRA_MIRRORS, ArrayList(mirrors))
			}
		}

		override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
			val mirrors = requireArguments().getStringArrayList(EXTRA_MIRRORS)!!
			return MaterialAlertDialogBuilder(requireContext()).setTitle(stringRes.select_mirror)
				.setItems(mirrors.toTypedArray()) { _, position ->
					(parentFragment as EditRepositoryFragment).setMirror(mirrors[position])
				}.setNegativeButton(stringRes.cancel, null).create()
		}
	}
}
