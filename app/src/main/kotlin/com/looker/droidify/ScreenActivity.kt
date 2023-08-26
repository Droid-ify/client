package com.looker.droidify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.looker.core.common.SdkCheck
import com.looker.core.common.extension.getDrawableFromAttr
import com.looker.core.common.extension.getPackageName
import com.looker.core.common.file.KParcelable
import com.looker.core.common.sdkAbove
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.getProperty
import com.looker.core.datastore.extension.getThemeRes
import com.looker.droidify.database.CursorOwner
import com.looker.droidify.ui.ScreenFragment
import com.looker.droidify.ui.app_detail.AppDetailFragment
import com.looker.droidify.ui.favourites.FavouritesFragment
import com.looker.droidify.ui.repository.EditRepositoryFragment
import com.looker.droidify.ui.repository.RepositoriesFragment
import com.looker.droidify.ui.repository.RepositoryFragment
import com.looker.droidify.ui.settings.SettingsFragment
import com.looker.droidify.ui.tabs_fragment.TabsFragment
import com.looker.installer.InstallManager
import com.looker.installer.model.installFrom
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
abstract class ScreenActivity : AppCompatActivity() {
	companion object {
		private const val STATE_FRAGMENT_STACK = "fragmentStack"
	}

	sealed interface SpecialIntent {
		data object Updates : SpecialIntent
		class Install(val packageName: String?, val cacheFileName: String?) : SpecialIntent
	}

	private val notificationPermission =
		registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

	@Inject
	lateinit var installer: InstallManager

	private class FragmentStackItem(
		val className: String, val arguments: Bundle?,
		val savedState: Fragment.SavedState?,
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

	@EntryPoint
	@InstallIn(SingletonComponent::class)
	interface CustomUserRepositoryInjector {
		fun userPreferencesRepository(): UserPreferencesRepository
	}

	private fun collectChange() {
		val hiltEntryPoint =
			EntryPointAccessors.fromApplication(
				this,
				CustomUserRepositoryInjector::class.java
			)
		val newPreferences = hiltEntryPoint.userPreferencesRepository().userPreferencesFlow
		lifecycleScope.launch {
			newPreferences.getProperty { theme to dynamicTheme }
				.collectIndexed { index, themeAndDynamic ->
					setTheme(
						resources.configuration.getThemeRes(
							themeAndDynamic.first,
							themeAndDynamic.second
						)
					)
					if (index > 0) recreate()
				}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		collectChange()
		super.onCreate(savedInstanceState)
		collectChange()
		val rootView = FrameLayout(this).apply { id = R.id.main_content }
		addContentView(
			rootView,
			ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT
			)
		)

		when {
			ContextCompat.checkSelfPermission(
				this,
				Manifest.permission.POST_NOTIFICATIONS
			) == PackageManager.PERMISSION_GRANTED -> {
			}
			shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
				sdkAbove(Build.VERSION_CODES.TIRAMISU) {
					notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
				}
			}
			else -> {
				sdkAbove(Build.VERSION_CODES.TIRAMISU) {
					notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
				}
			}
		}

		supportFragmentManager.addFragmentOnAttachListener { _, _ ->
			hideKeyboard()
		}

		if (savedInstanceState == null) {
			cursorOwner = CursorOwner()
			supportFragmentManager.commit {
				add(cursorOwner, CursorOwner::class.java.name)
			}
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
		if (SdkCheck.isR) {
			window.statusBarColor = resources.getColor(android.R.color.transparent, theme)
			window.navigationBarColor = resources.getColor(android.R.color.transparent, theme)
			WindowCompat.setDecorFitsSystemWindows(window, false)
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
		supportFragmentManager.commit {
			if (open != null) {
				setCustomAnimations(
					if (open) R.animator.slide_in else 0,
					if (open) R.animator.slide_in_keep else R.animator.slide_out
				)
			}
			setReorderingAllowed(true)
			replace(R.id.main_content, fragment)
		}
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
					navigateProduct(packageName)
					specialIntent.cacheFileName?.also { cacheFile ->
						val installItem = packageName installFrom cacheFile
						lifecycleScope.launch { installer + installItem }
					}
				}
				Unit
			}
		}::class
	}

	open fun handleIntent(intent: Intent?) {
		when (intent?.action) {
			Intent.ACTION_VIEW -> {
				val packageName = intent.getPackageName()
				if (!packageName.isNullOrEmpty()) {
					val fragment = currentFragment
					if (fragment !is AppDetailFragment || fragment.packageName != packageName) {
						navigateProduct(packageName)
					}
				}
			}
		}
	}

	internal fun navigateFavourites() = pushFragment(FavouritesFragment())
	internal fun navigateProduct(packageName: String) = pushFragment(AppDetailFragment(packageName))
	internal fun navigateRepositories() = pushFragment(RepositoriesFragment())
	internal fun navigatePreferences() = pushFragment(SettingsFragment.newInstance())
	internal fun navigateAddRepository() = pushFragment(EditRepositoryFragment(null))
	internal fun navigateRepository(repositoryId: Long) =
		pushFragment(RepositoryFragment(repositoryId))

	internal fun navigateEditRepository(repositoryId: Long) =
		pushFragment(EditRepositoryFragment(repositoryId))
}
