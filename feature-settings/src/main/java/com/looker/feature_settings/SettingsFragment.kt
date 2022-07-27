package com.looker.feature_settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.looker.core_common.BuildConfig
import com.looker.core_datastore.UserPreferences
import com.looker.core_datastore.UserPreferencesRepository
import com.looker.core_datastore.extension.autoSyncName
import com.looker.core_datastore.extension.installerName
import com.looker.core_datastore.extension.proxyName
import com.looker.core_datastore.extension.themeName
import com.looker.core_datastore.model.AutoSync
import com.looker.core_datastore.model.InstallerType
import com.looker.core_datastore.model.ProxyType
import com.looker.core_datastore.model.Theme
import com.looker.feature_settings.databinding.SettingsFragmentBinding
import kotlinx.coroutines.launch
import com.looker.core_common.R.dimen as dimenRes
import com.looker.core_common.R.string as stringRes

class SettingsFragment : Fragment() {

	companion object {
		fun newInstance() = SettingsFragment()
	}

	private val viewModel: SettingsViewModel by viewModels {
		SettingsViewModelFactory(UserPreferencesRepository(requireContext()))
	}
	private var _binding: SettingsFragmentBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		_binding = SettingsFragmentBinding.inflate(inflater, container, false)
		with(binding) {
			language.title.text = getString(stringRes.prefs_language_title)
			theme.title.text = getString(stringRes.theme)
			listAnimation.title.text = getString(stringRes.list_animation)
			listAnimation.content.text = getString(stringRes.list_animation_description)
			autoSync.title.text = getString(stringRes.sync_repositories_automatically)
			notifyUpdates.title.text = getString(stringRes.notify_about_updates)
			notifyUpdates.content.text = getString(stringRes.notify_about_updates_summary)
			unstableUpdates.title.text = getString(stringRes.unstable_updates)
			unstableUpdates.content.text = getString(stringRes.unstable_updates_summary)
			incompatibleUpdates.title.text = getString(stringRes.incompatible_versions)
			incompatibleUpdates.content.text = getString(stringRes.incompatible_versions_summary)
			proxyType.title.text = getString(stringRes.proxy_type)
			proxyHost.title.text = getString(stringRes.proxy_host)
			proxyPort.title.text = getString(stringRes.proxy_port)
			installer.title.text = getString(stringRes.installer)
			creditFoxy.title.text = "Based on Foxy Droid"
			creditFoxy.content.text = "FoxyDroid"
			droidify.title.text = "Droid-ify"
			droidify.content.text = "v0.4.8"
		}
		lifecycleScope.launch {
			viewModel.initialSetup.collect {
				updateUserPreference(it)
				setChangeListener()
				collectChanges()
			}
		}
		return binding.root
	}

	private fun <T> View.addSingleCorrectDialog(
		initialValue: T,
		values: List<T>,
		@StringRes title: Int,
		onClick: (T) -> Unit,
		valueToString: (T) -> String
	) = MaterialAlertDialogBuilder(context)
		.setTitle(title)
		.setSingleChoiceItems(
			values.map(valueToString).toTypedArray(),
			values.indexOf(initialValue)
		) { dialog, newValue ->
			dialog.dismiss()
			post {
				onClick(values.elementAt(newValue))
			}
		}
		.setNegativeButton(stringRes.cancel, null)
		.create()

	private fun View.addIntEditText(
		initialValue: Int,
		@StringRes title: Int,
		onFinish: (Int) -> Unit
	): AlertDialog {
		val scroll = NestedScrollView(context)
		val customEditText = TextInputEditText(context)
		customEditText.id = android.R.id.edit
		val paddingValue = context.resources.getDimension(dimenRes.shape_margin_large).toInt()
		scroll.setPadding(paddingValue, 0, paddingValue, 0)
		customEditText.setText(initialValue.toString())
		customEditText.hint = customEditText.text.toString()
		customEditText.text?.let { editable -> customEditText.setSelection(editable.length) }
		customEditText.requestFocus()
		scroll.addView(
			customEditText,
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
		)
		return MaterialAlertDialogBuilder(context)
			.setTitle(title)
			.setView(scroll)
			.setPositiveButton(stringRes.ok) { _, _ ->
				post { onFinish(customEditText.text.toString().toInt()) }
			}
			.setNegativeButton(stringRes.cancel, null)
			.create()
			.apply {
				window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
			}
	}

	private fun View.addStringEditText(
		initialValue: String,
		@StringRes title: Int,
		onFinish: (String) -> Unit
	): AlertDialog {
		val scroll = NestedScrollView(context)
		val customEditText = TextInputEditText(context)
		customEditText.id = android.R.id.edit
		val paddingValue = context.resources.getDimension(dimenRes.shape_margin_large).toInt()
		scroll.setPadding(paddingValue, 0, paddingValue, 0)
		customEditText.setText(initialValue)
		customEditText.hint = customEditText.text.toString()
		customEditText.text?.let { editable -> customEditText.setSelection(editable.length) }
		customEditText.requestFocus()
		scroll.addView(
			customEditText,
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT
		)
		return MaterialAlertDialogBuilder(context)
			.setTitle(title)
			.setView(scroll)
			.setPositiveButton(stringRes.ok) { _, _ ->
				post { onFinish(customEditText.text.toString()) }
			}
			.setNegativeButton(stringRes.cancel, null)
			.create()
			.apply {
				window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
			}
	}

	private fun collectChanges() {
		lifecycleScope.launch {
			viewModel.userPreferencesFlow.collect {
				updateUserPreference(it)
			}
		}
	}

	private fun setChangeListener() {
		with(binding) {
			with(viewModel) {
				listAnimation.checked.setOnCheckedChangeListener { _, checked ->
					setListAnimation(checked)
				}
				notifyUpdates.checked.setOnCheckedChangeListener { _, checked ->
					setNotifyUpdates(checked)
				}
				unstableUpdates.checked.setOnCheckedChangeListener { _, checked ->
					setUnstableUpdates(checked)
				}
				incompatibleUpdates.checked.setOnCheckedChangeListener { _, checked ->
					setIncompatibleUpdates(checked)
				}
			}
		}
	}

	private fun updateUserPreference(userPreferences: UserPreferences) {
		with(binding) {
			language.content.text = userPreferences.language
			language.root.setOnClickListener { view ->
				view.addSingleCorrectDialog(
					initialValue = userPreferences.language,
					values = BuildConfig.DETECTED_LOCALES.toList(),
					title = stringRes.prefs_language_title,
					onClick = { viewModel.setLanguage(it) },
					valueToString = { it }
				).show()
			}
			theme.content.text = context.themeName(userPreferences.theme)
			theme.root.setOnClickListener { view ->
				view.addSingleCorrectDialog(
					initialValue = userPreferences.theme,
					values = Theme.values().toList(),
					title = stringRes.theme,
					onClick = { viewModel.setTheme(it) },
					valueToString = { view.context.themeName(it) }
				).show()
			}
			listAnimation.checked.isChecked = userPreferences.listAnimation
			autoSync.content.text = context.autoSyncName(userPreferences.autoSync)
			autoSync.root.setOnClickListener { view ->
				view.addSingleCorrectDialog(
					initialValue = userPreferences.autoSync,
					values = AutoSync.values().toList(),
					title = stringRes.theme,
					onClick = { viewModel.setAutoSync(it) },
					valueToString = { view.context.autoSyncName(it) }
				).show()
			}
			notifyUpdates.checked.isChecked = userPreferences.notifyUpdate
			unstableUpdates.checked.isChecked = userPreferences.unstableUpdate
			incompatibleUpdates.checked.isChecked = userPreferences.incompatibleVersions
			proxyType.content.text = context.proxyName(userPreferences.proxyType)
			proxyType.root.setOnClickListener { view ->
				view.addSingleCorrectDialog(
					initialValue = userPreferences.proxyType,
					values = ProxyType.values().toList(),
					title = stringRes.theme,
					onClick = { viewModel.setProxyType(it) },
					valueToString = { view.context.proxyName(it) }
				).show()
			}
			proxyHost.root.isEnabled = userPreferences.proxyType != ProxyType.DIRECT
			proxyHost.title.isEnabled = userPreferences.proxyType != ProxyType.DIRECT
			proxyHost.content.isEnabled = userPreferences.proxyType != ProxyType.DIRECT
			proxyHost.content.text = userPreferences.proxyHost
			proxyHost.root.setOnClickListener { view ->
				view.addStringEditText(
					initialValue = userPreferences.proxyHost,
					title = stringRes.proxy_host,
					onFinish = { viewModel.setProxyHost(it) }
				).show()
			}
			proxyPort.root.isEnabled = userPreferences.proxyType != ProxyType.DIRECT
			proxyPort.title.isEnabled = userPreferences.proxyType != ProxyType.DIRECT
			proxyPort.content.isEnabled = userPreferences.proxyType != ProxyType.DIRECT
			proxyPort.content.text = userPreferences.proxyPort.toString()
			proxyPort.root.setOnClickListener { view ->
				view.addIntEditText(
					initialValue = userPreferences.proxyPort,
					title = stringRes.proxy_host,
					onFinish = { viewModel.setProxyPort(it) }
				).show()
			}
			installer.content.text = context.installerName(userPreferences.installerType)
			installer.root.setOnClickListener { view ->
				view.addSingleCorrectDialog(
					initialValue = userPreferences.installerType,
					values = InstallerType.values().toList(),
					title = stringRes.theme,
					onClick = { viewModel.setInstaller(it) },
					valueToString = { view.context.installerName(it) }
				).show()
			}
		}
	}
}