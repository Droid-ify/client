package com.looker.droidify.screen

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.looker.droidify.R
import com.looker.droidify.content.Preferences
import com.looker.droidify.database.CursorOwner
import com.looker.droidify.utility.KParcelable
import com.looker.droidify.utility.Utils.startPackageInstaller
import com.looker.droidify.utility.extension.resources.getDrawableFromAttr
import com.looker.droidify.utility.extension.text.nullIfEmpty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class ScreenActivity : FragmentActivity() {
    companion object {
        private const val STATE_FRAGMENT_STACK = "fragmentStack"
    }

    sealed class SpecialIntent {
        object Updates : SpecialIntent()
        class Install(val packageName: String?, val cacheFileName: String?) : SpecialIntent()
    }

    private class FragmentStackItem(
        val className: String, val arguments: Bundle?,
        val savedState: Fragment.SavedState?
    ) : KParcelable {
        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(className)
            dest.writeByte(if (arguments != null) 1 else 0)
            arguments?.writeToParcel(dest, flags)
            dest.writeByte(if (savedState != null) 1 else 0)
            savedState?.writeToParcel(dest, flags)
        }

        companion object {
            @Suppress("unused")
            @JvmField
            val CREATOR = KParcelable.creator {
                val className = it.readString()!!
                val arguments =
                    if (it.readByte().toInt() == 0) null else Bundle.CREATOR.createFromParcel(it)
                arguments?.classLoader = ScreenActivity::class.java.classLoader
                val savedState = if (it.readByte()
                        .toInt() == 0
                ) null else Fragment.SavedState.CREATOR.createFromParcel(it)
                FragmentStackItem(className, arguments, savedState)
            }
        }
    }

    lateinit var cursorOwner: CursorOwner
        private set

    private val fragmentStack = mutableListOf<FragmentStackItem>()

    private val currentFragment: Fragment?
        get() {
            supportFragmentManager.executePendingTransactions()
            return supportFragmentManager.findFragmentById(R.id.main_content)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Preferences[Preferences.Key.Theme].getResId(resources.configuration))
        super.onCreate(savedInstanceState)

        addContentView(
            FrameLayout(this).apply { id = R.id.main_content },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        if (savedInstanceState == null) {
            cursorOwner = CursorOwner()
            supportFragmentManager.beginTransaction()
                .add(cursorOwner, CursorOwner::class.java.name)
                .commit()
        } else {
            cursorOwner = supportFragmentManager
                .findFragmentByTag(CursorOwner::class.java.name) as CursorOwner
        }

        savedInstanceState?.getParcelableArrayList<FragmentStackItem>(STATE_FRAGMENT_STACK)
            ?.let { fragmentStack += it }
        if (savedInstanceState == null) {
            replaceFragment(TabsFragment(), null)
            if ((intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
                handleIntent(intent)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(STATE_FRAGMENT_STACK, ArrayList(fragmentStack))
    }

    override fun onBackPressed() {
        val currentFragment = currentFragment
        if (!(currentFragment is ScreenFragment && currentFragment.onBackPressed())) {
            hideKeyboard()
            if (!popFragment()) {
                super.onBackPressed()
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, open: Boolean?) {
        if (open != null) {
            currentFragment?.view?.translationZ =
                (if (open) Int.MIN_VALUE else Int.MAX_VALUE).toFloat()
        }
        supportFragmentManager
            .beginTransaction()
            .apply {
                if (open != null) {
                    setCustomAnimations(
                        if (open) R.animator.slide_in else 0,
                        if (open) R.animator.slide_in_keep else R.animator.slide_out
                    )
                }
            }
            .replace(R.id.main_content, fragment)
            .commit()
    }

    private fun pushFragment(fragment: Fragment) {
        currentFragment?.let {
            fragmentStack.add(
                FragmentStackItem(
                    it::class.java.name, it.arguments,
                    supportFragmentManager.saveFragmentInstanceState(it)
                )
            )
        }
        replaceFragment(fragment, true)
    }

    private fun popFragment(): Boolean {
        return fragmentStack.isNotEmpty() && run {
            val stackItem = fragmentStack.removeAt(fragmentStack.size - 1)
            val fragment = Class.forName(stackItem.className).newInstance() as Fragment
            stackItem.arguments?.let(fragment::setArguments)
            stackItem.savedState?.let(fragment::setInitialSavedState)
            replaceFragment(fragment, false)
            true
        }
    }

    private fun hideKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow((currentFocus ?: window.decorView).windowToken, 0)
    }

    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        hideKeyboard()
    }

    internal fun onToolbarCreated(toolbar: Toolbar) {
        if (fragmentStack.isNotEmpty()) {
            toolbar.navigationIcon =
                toolbar.context.getDrawableFromAttr(android.R.attr.homeAsUpIndicator)
            toolbar.setNavigationOnClickListener { onBackPressed() }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    protected val Intent.packageName: String?
        get() {
            val uri = data
            return when {
                uri?.scheme == "package" || uri?.scheme == "fdroid.app" -> {
                    uri.schemeSpecificPart?.nullIfEmpty()
                }
                uri?.scheme == "market" && uri.host == "details" -> {
                    uri.getQueryParameter("id")?.nullIfEmpty()
                }
                uri != null && uri.scheme in setOf("http", "https") -> {
                    val host = uri.host.orEmpty()
                    if (host == "f-droid.org" || host.endsWith(".f-droid.org")) {
                        uri.lastPathSegment?.nullIfEmpty()
                    } else {
                        null
                    }
                }
                else -> {
                    null
                }
            }
        }

    protected fun handleSpecialIntent(specialIntent: SpecialIntent) {
        when (specialIntent) {
            is SpecialIntent.Updates -> {
                if (currentFragment !is TabsFragment) {
                    fragmentStack.clear()
                    replaceFragment(TabsFragment(), true)
                }
                val tabsFragment = currentFragment as TabsFragment
                tabsFragment.selectUpdates()
            }
            is SpecialIntent.Install -> {
                val packageName = specialIntent.packageName
                if (!packageName.isNullOrEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        specialIntent.cacheFileName?.let { startPackageInstaller(it) }
                    }
                }
                Unit
            }
        }::class
    }

    open fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                val packageName = intent.packageName
                if (!packageName.isNullOrEmpty()) {
                    val fragment = currentFragment
                    if (fragment !is ProductFragment || fragment.packageName != packageName) {
                        navigateProduct(packageName)
                    }
                }
            }
        }
    }

    internal fun navigateProduct(packageName: String) = pushFragment(ProductFragment(packageName))
    internal fun navigateRepositories() = pushFragment(RepositoriesFragment())
    internal fun navigatePreferences() = pushFragment(SettingsFragment())
    internal fun navigateAddRepository() = pushFragment(EditRepositoryFragment(null))
    internal fun navigateRepository(repositoryId: Long) =
        pushFragment(RepositoryFragment(repositoryId))

    internal fun navigateEditRepository(repositoryId: Long) =
        pushFragment(EditRepositoryFragment(repositoryId))
}
