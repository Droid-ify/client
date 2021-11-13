package com.looker.droidify.screen

import android.content.ClipboardManager
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.util.Base64
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.looker.droidify.R
import com.looker.droidify.database.Database
import com.looker.droidify.databinding.EditRepositoryBinding
import com.looker.droidify.entity.Repository
import com.looker.droidify.network.Downloader
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.utility.RxUtils
import com.looker.droidify.utility.Utils
import com.looker.droidify.utility.extension.resources.getColorFromAttr
import com.looker.droidify.utility.extension.text.nullIfEmpty
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.net.URI
import java.net.URL
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min

class EditRepositoryFragment() : ScreenFragment() {

    private var _editRepositoryBinding: EditRepositoryBinding? = null
    private val editRepositoryBinding get() = _editRepositoryBinding!!

    companion object {
        private const val EXTRA_REPOSITORY_ID = "repositoryId"

        private val checkPaths = listOf("", "fdroid/repo", "repo")
    }

    constructor(repositoryId: Long?) : this() {
        arguments = Bundle().apply {
            repositoryId?.let { putLong(EXTRA_REPOSITORY_ID, it) }
        }
    }

    private class Layout(view: EditRepositoryBinding) {
        val address = view.address
        val addressMirror = view.addressMirror
        val fingerprint = view.fingerprint
        val username = view.username
        val password = view.password
        val overlay = view.overlay
        val skip = view.skip
    }

    private val repositoryId: Long?
        get() = requireArguments().let {
            if (it.containsKey(EXTRA_REPOSITORY_ID))
                it.getLong(EXTRA_REPOSITORY_ID) else null
        }

    private lateinit var errorColorFilter: PorterDuffColorFilter

    private var saveMenuItem: MenuItem? = null
    private var layout: Layout? = null

    private val syncConnection = Connection(SyncService::class.java)
    private var checkDisposable: Disposable? = null

    private var takenAddresses = emptySet<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _editRepositoryBinding = EditRepositoryBinding.inflate(layoutInflater)

        syncConnection.bind(requireContext())

        screenActivity.onToolbarCreated(toolbar)
        toolbar.setTitle(if (repositoryId != null) R.string.edit_repository else R.string.add_repository)

        saveMenuItem = toolbar.menu.add(R.string.save)
            .setIcon(Utils.getToolbarIcon(toolbar.context, R.drawable.ic_save))
            .setEnabled(false)
            .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
            .setOnMenuItemClickListener {
                onSaveRepositoryClick(true)
                true
            }

        val content = fragmentBinding.fragmentContent
        errorColorFilter = PorterDuffColorFilter(
            content.context
                .getColorFromAttr(R.attr.colorError).defaultColor, PorterDuff.Mode.SRC_IN
        )

        content.addView(editRepositoryBinding.root)
        val layout = Layout(editRepositoryBinding)
        this.layout = layout

        layout.fingerprint.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

            private val validChar: (Char) -> Boolean =
                { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

            private fun logicalPosition(s: String, position: Int): Int {
                return if (position > 0) s.asSequence().take(position)
                    .count(validChar) else position
            }

            private fun realPosition(s: String, position: Int): Int {
                return if (position > 0) {
                    var left = position
                    val index = s.indexOfFirst {
                        validChar(it) && run {
                            left -= 1
                            left <= 0
                        }
                    }
                    if (index >= 0) min(index + 1, s.length) else s.length
                } else {
                    position
                }
            }

            override fun afterTextChanged(s: Editable) {
                val inputString = s.toString()
                val outputString = inputString.uppercase(Locale.US)
                    .filter(validChar).windowed(2, 2, true).take(32).joinToString(separator = " ")
                if (inputString != outputString) {
                    val inputStart = logicalPosition(inputString, Selection.getSelectionStart(s))
                    val inputEnd = logicalPosition(inputString, Selection.getSelectionEnd(s))
                    s.replace(0, s.length, outputString)
                    Selection.setSelection(
                        s,
                        realPosition(outputString, inputStart),
                        realPosition(outputString, inputEnd)
                    )
                }
            }
        })

        if (savedInstanceState == null) {
            val repository = repositoryId?.let(Database.RepositoryAdapter::get)
            if (repository == null) {
                val clipboardManager =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val text = clipboardManager.primaryClip
                    ?.let { if (it.itemCount > 0) it else null }
                    ?.getItemAt(0)?.text?.toString().orEmpty()
                val (addressText, fingerprintText) = try {
                    val uri = Uri.parse(URL(text).toString())
                    val fingerprintText = uri.getQueryParameter("fingerprint")?.nullIfEmpty()
                        ?: uri.getQueryParameter("FINGERPRINT")?.nullIfEmpty()
                    Pair(
                        uri.buildUpon().path(uri.path?.pathCropped)
                            .query(null).fragment(null).build().toString(), fingerprintText
                    )
                } catch (e: Exception) {
                    Pair(null, null)
                }
                layout.address.setText(addressText?.nullIfEmpty() ?: layout.address.hint)
                layout.fingerprint.setText(fingerprintText)
            } else {
                layout.address.setText(repository.address)
                val mirrors = repository.mirrors.map { it.withoutKnownPath }
                if (mirrors.isNotEmpty()) {
                    layout.addressMirror.visibility = View.VISIBLE
                    layout.address.apply {
                        setPaddingRelative(
                            paddingStart, paddingTop,
                            paddingEnd + layout.addressMirror.layoutParams.width, paddingBottom
                        )
                    }
                    layout.addressMirror.setOnClickListener {
                        SelectMirrorDialog(mirrors)
                            .show(childFragmentManager, SelectMirrorDialog::class.java.name)
                    }
                }
                layout.fingerprint.setText(repository.fingerprint)
                val (usernameText, passwordText) = repository.authentication.nullIfEmpty()
                    ?.let { if (it.startsWith("Basic ")) it.substring(6) else null }
                    ?.let {
                        try {
                            Base64.decode(it, Base64.NO_WRAP).toString(Charset.defaultCharset())
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }
                    ?.let {
                        val index = it.indexOf(':')
                        if (index >= 0) Pair(
                            it.substring(0, index),
                            it.substring(index + 1)
                        ) else null
                    }
                    ?: Pair(null, null)
                layout.username.setText(usernameText)
                layout.password.setText(passwordText)
            }
        }

        layout.address.addTextChangedListener(SimpleTextWatcher { invalidateAddress() })
        layout.fingerprint.addTextChangedListener(SimpleTextWatcher { invalidateFingerprint() })
        layout.username.addTextChangedListener(SimpleTextWatcher { invalidateUsernamePassword() })
        layout.password.addTextChangedListener(SimpleTextWatcher { invalidateUsernamePassword() })

        (layout.overlay.parent as ViewGroup).layoutTransition?.setDuration(200L)
        layout.overlay.background!!.apply {
            mutate()
            alpha = 0xcc
        }
        layout.skip.setOnClickListener {
            if (checkDisposable != null) {
                checkDisposable?.dispose()
                checkDisposable = null
                onSaveRepositoryClick(false)
            }
        }

        lifecycleScope.launch {
            val list = Database.RepositoryAdapter.getAll(null)
            takenAddresses = list.asSequence().filter { it.id != repositoryId }
                .flatMap { (it.mirrors + it.address).asSequence() }
                .map { it.withoutKnownPath }.toSet()
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
        checkDisposable?.dispose()
        checkDisposable = null
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
                R.string.already_exists
            } else {
                null
            }
        } else {
            R.string.invalid_address
        }
        addressError = addressErrorResId != null
        addressErrorResId?.let { layout.address.error = getString(it) }
        invalidateState()
    }

    private fun invalidateFingerprint() {
        val layout = layout!!
        val fingerprint = layout.fingerprint.text.toString().replace(" ", "")
        val fingerprintInvalid = fingerprint.isNotEmpty() && fingerprint.length != 64
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
        usernamePasswordError = usernameInvalid || usernameEmpty || passwordEmpty
        invalidateState()
    }

    private fun invalidateState() {
        val layout = layout!!
        saveMenuItem!!.isEnabled = !addressError && !fingerprintError &&
                !usernamePasswordError && checkDisposable == null
        layout.apply {
            sequenceOf(address, addressMirror, fingerprint, username, password)
                .forEach { it.isEnabled = checkDisposable == null }
        }
        layout.overlay.visibility = if (checkDisposable != null) View.VISIBLE else View.GONE
    }

    private val String.pathCropped: String
        get() {
            val index = indexOfLast { it != '/' }
            return if (index >= 0 && index < length - 1) substring(0, index + 1) else this
        }

    private val String.withoutKnownPath: String
        get() {
            val cropped = pathCropped
            val endsWith = checkPaths.asSequence().filter { it.isNotEmpty() }
                .sortedByDescending { it.length }.find { cropped.endsWith("/$it") }
            return if (endsWith != null) cropped.substring(
                0,
                cropped.length - endsWith.length - 1
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
                    uri.scheme,
                    uri.userInfo,
                    uri.host,
                    uri.port,
                    path,
                    uri.query,
                    uri.fragment
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
        if (checkDisposable == null) {
            val layout = layout!!
            val address = normalizeAddress(layout.address.text.toString())!!
            val fingerprint = layout.fingerprint.text.toString().replace(" ", "")
            val username = layout.username.text.toString().nullIfEmpty()
            val password = layout.password.text.toString().nullIfEmpty()
            val paths = sequenceOf("", "fdroid/repo", "repo")
            val authentication = username?.let { u ->
                password
                    ?.let { p ->
                        Base64.encodeToString(
                            "$u:$p".toByteArray(Charset.defaultCharset()),
                            Base64.NO_WRAP
                        )
                    }
            }
                ?.let { "Basic $it" }.orEmpty()

            if (check) {
                checkDisposable = paths
                    .fold(Single.just("")) { oldAddressSingle, checkPath ->
                        oldAddressSingle
                            .flatMap { oldAddress ->
                                if (oldAddress.isEmpty()) {
                                    val builder = Uri.parse(address).buildUpon()
                                        .let {
                                            if (checkPath.isEmpty()) it else it.appendEncodedPath(
                                                checkPath
                                            )
                                        }
                                    val newAddress = builder.build()
                                    val indexAddress = builder.appendPath("index.jar").build()
                                    RxUtils
                                        .callSingle {
                                            Downloader
                                                .createCall(
                                                    Request.Builder().method("HEAD", null)
                                                        .url(indexAddress.toString().toHttpUrl()),
                                                    authentication,
                                                    null
                                                )
                                        }
                                        .subscribeOn(Schedulers.io())
                                        .map { if (it.code == 200) newAddress.toString() else "" }
                                } else {
                                    Single.just(oldAddress)
                                }
                            }
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { result, throwable ->
                        checkDisposable = null
                        throwable?.printStackTrace()
                        val resultAddress =
                            result?.let { if (it.isEmpty()) null else it } ?: address
                        val allow = resultAddress == address || run {
                            layout.address.setText(resultAddress)
                            invalidateAddress(resultAddress)
                            !addressError
                        }
                        if (allow) {
                            onSaveRepositoryProceedInvalidate(
                                resultAddress,
                                fingerprint,
                                authentication
                            )
                        } else {
                            invalidateState()
                        }
                    }
                invalidateState()
            } else {
                onSaveRepositoryProceedInvalidate(address, fingerprint, authentication)
            }
        }
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
                requireActivity().onBackPressed()
            }
        } else {
            invalidateState()
        }
    }

    private class SimpleTextWatcher(private val callback: (Editable) -> Unit) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
        override fun afterTextChanged(s: Editable) = callback(s)
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
            return MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.select_mirror)
                .setItems(mirrors.toTypedArray()) { _, position ->
                    (parentFragment as EditRepositoryFragment)
                        .setMirror(mirrors[position])
                }
                .setNegativeButton(R.string.cancel, null)
                .create()
        }
    }
}
