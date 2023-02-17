package com.looker.droidify.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.looker.core.common.SdkCheck
import com.looker.core.common.extension.dp
import com.looker.core.common.extension.getDrawableFromAttr
import com.looker.core.common.extension.getPackageName
import com.looker.core.common.extension.systemBarsMargin
import com.looker.core.common.sdkAbove
import com.looker.core.data.utils.NetworkMonitor
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.distinctMap
import com.looker.core.datastore.extension.getThemeRes
import com.looker.core.model.newer.toPackageName
import com.looker.droidify.R
import com.looker.droidify.database.CursorOwner
import com.looker.droidify.ui.app_detail.AppDetailFragment
import com.looker.droidify.ui.favourites.FavouritesFragment
import com.looker.droidify.ui.tabs_fragment.TabsFragment
import com.looker.feature_settings.SettingsFragment
import com.looker.installer.Installer
import com.looker.installer.model.InstallItem
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

	sealed interface SpecialIntent {
		object Updates : SpecialIntent
		class Install(val packageName: String?, val cacheFileName: String?) : SpecialIntent
	}

	private val notificationPermission =
		registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

	@Inject
	lateinit var networkMonitor: NetworkMonitor

	@Inject
	lateinit var installer: Installer

	lateinit var cursorOwner: CursorOwner
		private set

	private val currentFragment: Fragment?
		get() = supportFragmentManager.findFragmentById(R.id.main_content)

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
			newPreferences.distinctMap { it.theme to it.dynamicTheme }
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
		val noInternetSnackbar =
			Snackbar.make(rootView, R.string.no_internet, Snackbar.LENGTH_SHORT)
				.setAnimationMode(Snackbar.ANIMATION_MODE_FADE)
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.CREATED) {
				networkMonitor.isOnline.collect { isOnline ->
					if (!isOnline) noInternetSnackbar.show()
				}
			}
		}

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
		if (savedInstanceState == null) {
			replaceFragment(TabsFragment(), null)
			if ((intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
				handleIntent(intent)
			}
		}
		noInternetSnackbar.view.systemBarsMargin(16.dp)
		if (SdkCheck.isR) {
			window.statusBarColor = resources.getColor(android.R.color.transparent, theme)
			window.navigationBarColor = resources.getColor(android.R.color.transparent, theme)
			WindowCompat.setDecorFitsSystemWindows(window, false)
		}
	}

	override fun onBackPressed() {
		val currentFragment = currentFragment
		if (!(currentFragment is ScreenFragment && currentFragment.onBackPressed())) {
			hideKeyboard()
			if (supportFragmentManager.backStackEntryCount > 0) {
				supportFragmentManager.popBackStack()
			} else {
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
			setReorderingAllowed(true)
			if (open != null) {
				setCustomAnimations(
					if (open) R.animator.slide_in else 0,
					if (open) R.animator.slide_in_keep else R.animator.slide_out
				)
			}
			replace(R.id.main_content, fragment)
		}
	}

	private fun pushFragment(fragment: Fragment) {
		currentFragment?.view?.translationZ = (Int.MIN_VALUE).toFloat()
		supportFragmentManager.commit {
			addToBackStack(fragment.tag)
			setReorderingAllowed(true)
			setCustomAnimations(
				R.animator.slide_in,
				R.animator.slide_in_keep
			)
			replace(R.id.main_content, fragment)
		}
	}

	private fun hideKeyboard() {
		(getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
			?.hideSoftInputFromWindow((currentFocus ?: window.decorView).windowToken, 0)
	}

	internal fun onToolbarCreated(toolbar: Toolbar) {
		if (supportFragmentManager.backStackEntryCount > 0) {
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
					replaceFragment(TabsFragment(), true)
				}
				val tabsFragment = currentFragment as TabsFragment
				tabsFragment.selectUpdates()
			}
			is SpecialIntent.Install -> {
				val packageName = specialIntent.packageName
				if (!packageName.isNullOrEmpty()) {
					navigateProduct(packageName)
					specialIntent.cacheFileName?.let { cacheFile ->
						lifecycleScope.launch {
							val installItem = InstallItem(packageName.toPackageName(), cacheFile)
							installer + installItem
						}
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
