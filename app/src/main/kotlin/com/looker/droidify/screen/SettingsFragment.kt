package com.looker.droidify.screen

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.circularreveal.CircularRevealFrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.looker.droidify.BuildConfig
import com.looker.droidify.R
import com.looker.droidify.content.Preferences
import com.looker.droidify.databinding.PreferenceItemBinding
import com.looker.droidify.utility.Utils.getLocaleOfCode
import com.looker.droidify.utility.Utils.languagesList
import com.looker.droidify.utility.Utils.translateLocale
import com.looker.droidify.utility.extension.resources.*
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.launch

class SettingsFragment : ScreenFragment() {

	private var preferenceBinding: PreferenceItemBinding? = null
	private val preferences = mutableMapOf<Preferences.Key<*>, Preference<*>>()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		preferenceBinding = PreferenceItemBinding.inflate(layoutInflater)
		screenActivity.onToolbarCreated(toolbar)
		collapsingToolbar.title = getString(R.string.settings)

		val content = fragmentBinding.fragmentContent
		val scroll = NestedScrollView(content.context)
		scroll.clipToPadding = false
		ViewCompat.setOnApplyWindowInsetsListener(scroll) { list, windowInsets ->
			val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			list.updatePadding(bottom = insets.bottom)
			WindowInsetsCompat.CONSUMED
		}
		scroll.id = R.id.preferences_list
		scroll.isFillViewport = true
		content.addView(
			scroll,
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.MATCH_PARENT
		)
		val scrollLayout = CircularRevealFrameLayout(content.context)
		scroll.addView(
			scrollLayout,
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
		)
		val preferences = LinearLayoutCompat(scrollLayout.context)
		preferences.orientation = LinearLayoutCompat.VERTICAL
		scrollLayout.addView(
			preferences,
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
		)

		preferences.addCategory(requireContext().getString(R.string.prefs_personalization)) {
			addList(
				Preferences.Key.Language,
				context.getString(R.string.prefs_language_title),
				languagesList
			) { translateLocale(context.getLocaleOfCode(it)) }
			addEnumeration(Preferences.Key.Theme, getString(R.string.theme)) {
				when (it) {
					is Preferences.Theme.System -> getString(R.string.system)
					is Preferences.Theme.AmoledSystem -> getString(R.string.system) + " " + getString(
						R.string.amoled
					)
					is Preferences.Theme.Light -> getString(R.string.light)
					is Preferences.Theme.Dark -> getString(R.string.dark)
					is Preferences.Theme.Amoled -> getString(R.string.amoled)
				}
			}
			addSwitch(
				Preferences.Key.ListAnimation, getString(R.string.list_animation),
				getString(R.string.list_animation_description)
			)
		}
		preferences.addCategory(getString(R.string.updates)) {
			addEnumeration(
				Preferences.Key.AutoSync,
				getString(R.string.sync_repositories_automatically)
			) {
				when (it) {
					Preferences.AutoSync.Never -> getString(R.string.never)
					Preferences.AutoSync.Wifi -> getString(R.string.only_on_wifi)
					Preferences.AutoSync.Always -> getString(R.string.always)
				}
			}
			addSwitch(
				Preferences.Key.UpdateNotify, getString(R.string.notify_about_updates),
				getString(R.string.notify_about_updates_summary)
			)
			addSwitch(
				Preferences.Key.UpdateUnstable, getString(R.string.unstable_updates),
				getString(R.string.unstable_updates_summary)
			)
			addSwitch(
				Preferences.Key.IncompatibleVersions, getString(R.string.incompatible_versions),
				getString(R.string.incompatible_versions_summary)
			)
		}
		preferences.addCategory(getString(R.string.proxy)) {
			addEnumeration(Preferences.Key.ProxyType, getString(R.string.proxy_type)) {
				when (it) {
					is Preferences.ProxyType.Direct -> getString(R.string.no_proxy)
					is Preferences.ProxyType.Http -> getString(R.string.http_proxy)
					is Preferences.ProxyType.Socks -> getString(R.string.socks_proxy)
				}
			}
			addEditString(Preferences.Key.ProxyHost, getString(R.string.proxy_host))
			addEditInt(Preferences.Key.ProxyPort, getString(R.string.proxy_port), 1..65535)
		}
		preferences.addCategory(getString(R.string.install_types)) {
			addSwitch(
				Preferences.Key.RootPermission, getString(R.string.root_permission),
				getString(R.string.root_permission_description)
			)
		}
		preferences.addCategory(getString(R.string.credits)) {
			addText(
				title = "Based on Foxy-Droid",
				summary = "FoxyDroid",
				url = "https://github.com/kitsunyan/foxy-droid/"
			)
			addText(
				title = getString(R.string.application_name),
				summary = "v ${BuildConfig.VERSION_NAME}",
				url = "https://github.com/iamlooker/Droid-ify/"
			)
		}

		lifecycleScope.launch {
			Preferences.subject
				.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
				.collect { updatePreference(it) }
		}
		updatePreference(null)
	}

	private fun LinearLayoutCompat.addText(title: String, summary: String, url: String) {
		val text = MaterialTextView(context)
		val subText = MaterialTextView(context)
		text.text = title
		subText.text = summary
		text.setTextSizeScaled(16)
		subText.setTextSizeScaled(14)
		resources.sizeScaled(16).let {
			text.setPadding(it, it, 5, 5)
			subText.setPadding(it, 5, 5, 25)
		}
		addView(
			text,
			LinearLayoutCompat.LayoutParams.MATCH_PARENT,
			LinearLayoutCompat.LayoutParams.WRAP_CONTENT
		)
		addView(
			subText,
			LinearLayoutCompat.LayoutParams.MATCH_PARENT,
			LinearLayoutCompat.LayoutParams.WRAP_CONTENT
		)
		text.setOnClickListener { openURI(url.toUri()) }
		subText.setOnClickListener { openURI(url.toUri()) }
	}

	private fun openURI(url: Uri) {
		startActivity(Intent(Intent.ACTION_VIEW, url))
	}

	override fun onDestroyView() {
		super.onDestroyView()
		preferences.clear()
		preferenceBinding = null
	}

	private fun updatePreference(key: Preferences.Key<*>?) {
		if (key != null) {
			preferences[key]?.update()
		}
		if (key == null || key == Preferences.Key.ProxyType) {
			val enabled = when (Preferences[Preferences.Key.ProxyType]) {
				is Preferences.ProxyType.Direct -> false
				is Preferences.ProxyType.Http, is Preferences.ProxyType.Socks -> true
			}
			preferences[Preferences.Key.ProxyHost]?.setEnabled(enabled)
			preferences[Preferences.Key.ProxyPort]?.setEnabled(enabled)
		}
		if (key == Preferences.Key.RootPermission) {
			preferences[Preferences.Key.RootPermission]?.setEnabled(
				Shell.getCachedShell()?.isRoot
					?: Shell.getShell().isRoot
			)
		}
		if (key == Preferences.Key.Theme) {
			requireActivity().recreate()
		}
	}

	private fun LinearLayoutCompat.addCategory(
		title: String,
		callback: LinearLayoutCompat.() -> Unit,
	) {
		val text = MaterialTextView(context)
		text.typeface = TypefaceExtra.medium
		text.setTextSizeScaled(14)
		text.setTextColor(text.context.getColorFromAttr(R.attr.colorPrimary))
		text.text = title
		resources.sizeScaled(16).let { text.setPadding(it, it, it, 0) }
		addView(
			text,
			LinearLayoutCompat.LayoutParams.MATCH_PARENT,
			LinearLayoutCompat.LayoutParams.WRAP_CONTENT
		)
		callback()
	}

	private fun <T> LinearLayoutCompat.addPreference(
		key: Preferences.Key<T>, title: String,
		summaryProvider: () -> String, dialogProvider: ((Context) -> AlertDialog)?,
	): Preference<T> {
		val preference =
			Preference(key, this@SettingsFragment, this, title, summaryProvider, dialogProvider)
		preferences[key] = preference
		return preference
	}

	private fun LinearLayoutCompat.addSwitch(
		key: Preferences.Key<Boolean>,
		title: String,
		summary: String,
	) {
		val preference = addPreference(key, title, { summary }, null)
		preference.check.visibility = View.VISIBLE
		preference.view.setOnClickListener { Preferences[key] = !Preferences[key] }
		preference.setCallback { preference.check.isChecked = Preferences[key] }
	}

	private fun <T> LinearLayoutCompat.addEdit(
		key: Preferences.Key<T>, title: String, valueToString: (T) -> String,
		stringToValue: (String) -> T?, configureEdit: (TextInputEditText) -> Unit,
	) {
		addPreference(key, title, { valueToString(Preferences[key]) }) { it ->
			val scroll = NestedScrollView(it)
			scroll.resources.sizeScaled(20).let { scroll.setPadding(it, 0, it, 0) }
			val edit = TextInputEditText(it)
			configureEdit(edit)
			edit.id = android.R.id.edit
			edit.resources.sizeScaled(16)
				.let { edit.setPadding(edit.paddingLeft, it, edit.paddingRight, it) }
			edit.setText(valueToString(Preferences[key]))
			edit.hint = edit.text.toString()
			edit.text?.let { editable -> edit.setSelection(editable.length) }
			edit.requestFocus()
			scroll.addView(
				edit,
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			)
			MaterialAlertDialogBuilder(it)
				.setTitle(title)
				.setView(scroll)
				.setPositiveButton(R.string.ok) { _, _ ->
					val value = stringToValue(edit.text.toString()) ?: key.default.value
					post { Preferences[key] = value }
				}
				.setNegativeButton(R.string.cancel, null)
				.create()
				.apply {
					window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
				}
		}
	}

	private fun LinearLayoutCompat.addEditString(key: Preferences.Key<String>, title: String) {
		addEdit(key, title, { it }, { it }, { })
	}

	private fun LinearLayoutCompat.addEditInt(
		key: Preferences.Key<Int>,
		title: String,
		range: IntRange?,
	) {
		addEdit(key, title, { it.toString() }, { it.toIntOrNull() }) {
			it.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
			if (range != null) it.filters =
				arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
					val value = (dest.substring(0, dstart) + source.substring(start, end) +
							dest.substring(dend, dest.length)).toIntOrNull()
					if (value != null && value in range) null else ""
				})
		}
	}

	private fun <T : Preferences.Enumeration<T>> LinearLayoutCompat.addEnumeration(
		key: Preferences.Key<T>,
		title: String,
		valueToString: (T) -> String,
	) {
		addPreference(key, title, { valueToString(Preferences[key]) }) {
			val values = key.default.value.values
			MaterialAlertDialogBuilder(it)
				.setTitle(title)
				.setSingleChoiceItems(
					values.map(valueToString).toTypedArray(),
					values.indexOf(Preferences[key])
				) { dialog, which ->
					dialog.dismiss()
					post { Preferences[key] = values[which] }
				}
				.setNegativeButton(R.string.cancel, null)
				.create()
		}
	}

	private fun <T> LinearLayoutCompat.addList(
		key: Preferences.Key<T>,
		title: String,
		values: List<T>,
		valueToString: (T) -> String,
	) {
		addPreference(key, title, { valueToString(Preferences[key]) }) {
			MaterialAlertDialogBuilder(it)
				.setTitle(title)
				.setSingleChoiceItems(
					values.map(valueToString).toTypedArray(),
					values.indexOf(Preferences[key])
				) { dialog, which ->
					dialog.dismiss()
					post { Preferences[key] = values[which] }
				}
				.setNegativeButton(R.string.cancel, null)
				.create()
		}
	}

	private class Preference<T>(
		private val key: Preferences.Key<T>,
		fragment: Fragment,
		parent: ViewGroup,
		titleText: String,
		private val summaryProvider: () -> String,
		private val dialogProvider: ((Context) -> AlertDialog)?,
	) {
		val view = parent.inflate(R.layout.preference_item)
		val title = view.findViewById<MaterialTextView>(R.id.title)!!
		val summary = view.findViewById<MaterialTextView>(R.id.summary)!!
		val check = view.findViewById<SwitchMaterial>(R.id.check)!!

		private var callback: (() -> Unit)? = null

		init {
			title.text = titleText
			parent.addView(view)
			if (dialogProvider != null) {
				view.setOnClickListener {
					PreferenceDialog(key.name)
						.show(
							fragment.childFragmentManager,
							"${PreferenceDialog::class.java.name}.${key.name}"
						)
				}
			}
			update()
		}

		fun setCallback(callback: () -> Unit) {
			this.callback = callback
			update()
		}

		fun setEnabled(enabled: Boolean) {
			view.isEnabled = enabled
			title.isEnabled = enabled
			summary.isEnabled = enabled
			check.isEnabled = enabled
		}

		fun update() {
			summary.text = summaryProvider()
			summary.visibility = if (summary.text.isNotEmpty()) View.VISIBLE else View.GONE
			callback?.invoke()
		}

		fun createDialog(context: Context): AlertDialog {
			return dialogProvider!!(context)
		}
	}

	class PreferenceDialog() : DialogFragment() {
		companion object {
			private const val EXTRA_KEY = "key"
		}

		constructor(key: String) : this() {
			arguments = Bundle().apply {
				putString(EXTRA_KEY, key)
			}
		}

		override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
			val preferences = (parentFragment as SettingsFragment).preferences
			val key = requireArguments().getString(EXTRA_KEY)!!
				.let { name -> preferences.keys.find { it.name == name }!! }
			val preference = preferences[key]!!
			return preference.createDialog(requireContext())
		}
	}
}
