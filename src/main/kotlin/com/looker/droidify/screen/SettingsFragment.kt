package com.looker.droidify.screen

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.looker.droidify.R
import com.looker.droidify.content.Preferences
import com.looker.droidify.utility.extension.resources.*
import io.reactivex.rxjava3.disposables.Disposable


class SettingsFragment : ScreenFragment() {
    private val preferences = mutableMapOf<Preferences.Key<*>, Preference<*>>()
    private var disposable: Disposable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)!!
        screenActivity.onToolbarCreated(toolbar)
        toolbar.setTitle(R.string.settings)

        val content = view.findViewById<FrameLayout>(R.id.fragment_content)!!
        val scroll = ScrollView(content.context)
        scroll.id = R.id.preferences_list
        scroll.isFillViewport = true
        content.addView(
            scroll,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        val scrollLayout = FrameLayout(content.context)
        scroll.addView(
            scrollLayout,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val preferences = LinearLayout(scrollLayout.context)
        preferences.orientation = LinearLayout.VERTICAL
        scrollLayout.addView(
            preferences,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

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
        preferences.addCategory(getString(R.string.other)) {
            addEnumeration(Preferences.Key.Theme, getString(R.string.theme)) {
                when (it) {
                    is Preferences.Theme.System -> getString(R.string.system)
                    is Preferences.Theme.Light -> getString(R.string.light)
                    is Preferences.Theme.Dark -> getString(R.string.dark)
                }
            }
            addSwitch(
                Preferences.Key.IncompatibleVersions, getString(R.string.incompatible_versions),
                getString(R.string.incompatible_versions_summary)
            )
        }
        // Adding Credits to Foxy
        //TODO "Fix Linking"

        preferences.addCategory("Credits") {
            addText(
                title = "Based on an App by kitsunyan",
                summary = "FoxyDroid"
            ).also {
                setOnClickListener {
                    openURI(urlToSite = "https://github.com/kitsunyan/foxy-droid/")
                }
            }
            addText(
                title = "Droid-ify",
                summary = "Iamlooker"
            )
        }

        // End Credits

        disposable = Preferences.observable.subscribe(this::updatePreference)
        updatePreference(null)
    }

    // Add Text for Credits

    private fun LinearLayout.addText(title: String, summary: String) {
        val text = TextView(context)
        val subText = TextView(context)
        text.text = title
        subText.text = summary
        text.textSize = 16F
        subText.textSize = 14F
        resources.sizeScaled(16).let { text.setPadding(it, it, 5, 5) }
        resources.sizeScaled(16).let { subText.setPadding(it, 5, 5, 5) }
        addView(
            text,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        addView(
            subText,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun openURI(urlToSite: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToSite))
        startActivity(browserIntent)
    }
    // End Add Text for Credits

    override fun onDestroyView() {
        super.onDestroyView()

        preferences.clear()
        disposable?.dispose()
        disposable = null
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
        if (key == Preferences.Key.Theme) {
            requireActivity().recreate()
        }
    }

    private fun LinearLayout.addCategory(title: String, callback: LinearLayout.() -> Unit) {
        val text = TextView(context)
        text.typeface = TypefaceExtra.medium
        text.setTextSizeScaled(14)
        text.setTextColor(text.context.getColorFromAttr(android.R.attr.colorAccent))
        text.text = title
        resources.sizeScaled(16).let { text.setPadding(it, it, it, 0) }
        addView(
            text,
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        callback()
    }

    private fun <T> LinearLayout.addPreference(
        key: Preferences.Key<T>, title: String,
        summaryProvider: () -> String, dialogProvider: ((Context) -> AlertDialog)?
    ): Preference<T> {
        val preference =
            Preference(key, this@SettingsFragment, this, title, summaryProvider, dialogProvider)
        preferences[key] = preference
        return preference
    }

    private fun LinearLayout.addSwitch(
        key: Preferences.Key<Boolean>,
        title: String,
        summary: String
    ) {
        val preference = addPreference(key, title, { summary }, null)
        preference.check.visibility = View.VISIBLE
        preference.view.setOnClickListener { Preferences[key] = !Preferences[key] }
        preference.setCallback { preference.check.isChecked = Preferences[key] }
    }

    private fun <T> LinearLayout.addEdit(
        key: Preferences.Key<T>, title: String, valueToString: (T) -> String,
        stringToValue: (String) -> T?, configureEdit: (EditText) -> Unit
    ) {
        addPreference(key, title, { valueToString(Preferences[key]) }) { it ->
            val scroll = ScrollView(it)
            scroll.resources.sizeScaled(20).let { scroll.setPadding(it, 0, it, 0) }
            val edit = EditText(it)
            configureEdit(edit)
            edit.id = android.R.id.edit
            edit.setTextSizeScaled(16)
            edit.resources.sizeScaled(16)
                .let { edit.setPadding(edit.paddingLeft, it, edit.paddingRight, it) }
            edit.setText(valueToString(Preferences[key]))
            edit.hint = edit.text.toString()
            edit.setSelection(edit.text.length)
            edit.requestFocus()
            scroll.addView(
                edit,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            AlertDialog.Builder(it)
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

    private fun LinearLayout.addEditString(key: Preferences.Key<String>, title: String) {
        addEdit(key, title, { it }, { it }, { })
    }

    private fun LinearLayout.addEditInt(
        key: Preferences.Key<Int>,
        title: String,
        range: IntRange?
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

    private fun <T : Preferences.Enumeration<T>> LinearLayout.addEnumeration(
        key: Preferences.Key<T>,
        title: String,
        valueToString: (T) -> String
    ) {
        addPreference(key, title, { valueToString(Preferences[key]) }) {
            val values = key.default.value.values
            AlertDialog.Builder(it)
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
        private val dialogProvider: ((Context) -> AlertDialog)?
    ) {
        val view = parent.inflate(R.layout.preference_item)
        val title = view.findViewById<TextView>(R.id.title)!!
        val summary = view.findViewById<TextView>(R.id.summary)!!
        val check = view.findViewById<Switch>(R.id.check)!!

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
