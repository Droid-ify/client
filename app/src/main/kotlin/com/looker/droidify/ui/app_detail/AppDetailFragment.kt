package com.looker.droidify.ui.app_detail

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.looker.core.common.extension.firstItemVisible
import com.looker.core.common.extension.systemBarsPadding
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.model.*
import com.looker.core.model.newer.toPackageName
import com.looker.droidify.content.ProductPreferences
import com.looker.droidify.database.Database
import com.looker.droidify.service.Connection
import com.looker.droidify.service.DownloadService
import com.looker.droidify.ui.MessageDialog
import com.looker.droidify.ui.ScreenFragment
import com.looker.droidify.ui.screenshots.ScreenshotsFragment
import com.looker.droidify.utility.Utils
import com.looker.droidify.utility.Utils.startUpdate
import com.looker.droidify.utility.extension.android.getApplicationInfoCompat
import com.looker.droidify.utility.extension.screenActivity
import com.looker.installer.InstallManager
import com.looker.installer.model.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.looker.core.common.R.string as stringRes

@AndroidEntryPoint
class AppDetailFragment() : ScreenFragment(), AppDetailAdapter.Callbacks {
	companion object {
		private const val EXTRA_PACKAGE_NAME = "packageName"
		private const val STATE_LAYOUT_MANAGER = "layoutManager"
		private const val STATE_ADAPTER = "adapter"
	}

	constructor(packageName: String) : this() {
		arguments = Bundle().apply {
			putString(EXTRA_PACKAGE_NAME, packageName)
		}
	}

	private enum class Action(
		val id: Int,
		val adapterAction: AppDetailAdapter.Action
	) {
		INSTALL(1, AppDetailAdapter.Action.INSTALL),
		UPDATE(2, AppDetailAdapter.Action.UPDATE),
		LAUNCH(3, AppDetailAdapter.Action.LAUNCH),
		DETAILS(4, AppDetailAdapter.Action.DETAILS),
		UNINSTALL(5, AppDetailAdapter.Action.UNINSTALL),
		SHARE(6, AppDetailAdapter.Action.SHARE)
	}

	private class Installed(
		val installedItem: InstalledItem, val isSystem: Boolean,
		val launcherActivities: List<Pair<String, String>>
	)

	val packageName: String
		get() = requireArguments().getString(EXTRA_PACKAGE_NAME)!!

	@Inject
	lateinit var installer: InstallManager

	@Inject
	lateinit var userPreferencesRepository: UserPreferencesRepository

	private val viewModel: AppDetailViewModel by viewModels()

	private var layoutManagerState: LinearLayoutManager.SavedState? = null

	private var actions = Pair(emptySet<Action>(), null as Action?)
	private var products =
		emptyList<Pair<Product, Repository>>()
	private var installed: Installed? = null
	private var downloading = false
	private var installing = false

	private var recyclerView: RecyclerView? = null

	private val downloadConnection = Connection(
		serviceClass = DownloadService::class.java,
		onBind = { _, binder ->
			binder.stateFlow
				.filter { it.packageName == packageName }
				.onEach { updateDownloadState(it) }
				.launchIn(lifecycleScope)
		}
	)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		screenActivity.onToolbarCreated(toolbar)
		toolbar.menu.apply {
			for (action in Action.values()) {
				add(0, action.id, 0, action.adapterAction.titleResId)
					.setIcon(Utils.getToolbarIcon(toolbar.context, action.adapterAction.iconResId))
					.setVisible(false)
					.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
					.setOnMenuItemClickListener {
						onActionClick(action.adapterAction)
						true
					}
			}
		}

		val content = fragmentBinding.fragmentContent
		content.addView(RecyclerView(content.context).apply {
			id = android.R.id.list
			this.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
			isMotionEventSplittingEnabled = false
			isVerticalScrollBarEnabled = false
			val adapter = AppDetailAdapter(this@AppDetailFragment)
			this.adapter = adapter
			(itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
			savedInstanceState?.getParcelable<AppDetailAdapter.SavedState>(STATE_ADAPTER)
				?.let(adapter::restoreState)
			layoutManagerState = savedInstanceState?.getParcelable(STATE_LAYOUT_MANAGER)
			recyclerView = this
		})
		recyclerView?.systemBarsPadding()
		var first = true
		viewLifecycleOwner.lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.RESUMED) {
				launch {
					flowOf(Unit)
						.onCompletion { if (it == null) emitAll(Database.flowCollection(Database.Subject.Products)) }
						.map { Database.ProductAdapter.get(packageName, null) }
						.map { products ->
							Database.RepositoryAdapter.getAll(null).associateBy { it.id }.let {
								products.mapNotNull { product ->
									it[product.repositoryId]?.let {
										product to it
									}
								}
							}
						}
						.map { it to Database.InstalledAdapter.get(packageName, null) }
						.collectLatest { (productRepo, installedItem) ->
							val firstChanged = first
							first = false
							val productChanged = products != productRepo
							val installedItemChanged =
								installed?.installedItem != installedItem
							if (firstChanged || productChanged || installedItemChanged) {
								layoutManagerState?.let {
									recyclerView?.layoutManager!!.onRestoreInstanceState(it)
								}
								layoutManagerState = null
								if (firstChanged || productChanged) {
									products = productRepo
								}
								if (firstChanged || installedItemChanged) {
									installed = installedItem?.let {
										val packageManager = requireContext().packageManager
										val isSystem = try {
											((packageManager.getApplicationInfoCompat(packageName)
												.flags) and ApplicationInfo.FLAG_SYSTEM) != 0
										} catch (e: Exception) {
											false
										}
										val launcherActivities =
											if (packageName == requireContext().packageName) {
												// Don't allow to launch self
												emptyList()
											} else {
												packageManager
													.queryIntentActivities(
														Intent(Intent.ACTION_MAIN).addCategory(
															Intent.CATEGORY_LAUNCHER
														), 0
													)
													.asSequence()
													.mapNotNull { resolveInfo -> resolveInfo.activityInfo }
													.filter { activityInfo -> activityInfo.packageName == packageName }
													.mapNotNull { activityInfo ->
														val label = try {
															activityInfo.loadLabel(packageManager)
																.toString()
														} catch (e: Exception) {
															e.printStackTrace()
															null
														}
														label?.let { labelName ->
															activityInfo.name to labelName
														}
													}
													.toList()
											}
										Installed(it, isSystem, launcherActivities)
									}
								}
								val recyclerView = recyclerView!!
								val adapter = recyclerView.adapter as AppDetailAdapter

								updateButtons()
								adapter.setProducts(
									recyclerView.context,
									packageName,
									productRepo,
									installedItem,
									userPreferencesRepository.fetchInitialPreferences()
								)
							}
						}
				}
				launch {
					viewModel.installerState.collect { updateInstallState(it) }
				}
				launch {
					recyclerView?.firstItemVisible?.collect { isFirstItemVisible ->
						updateToolbarButtons()
						toolbar.title = if (!isFirstItemVisible) products[0].first.name
						else getString(stringRes.application)
					}
				}
			}
		}

		downloadConnection.bind(requireContext())
	}

	override fun onDestroyView() {
		super.onDestroyView()
		recyclerView = null

		downloadConnection.unbind(requireContext())
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)

		val layoutManagerState =
			layoutManagerState ?: recyclerView?.layoutManager?.onSaveInstanceState()
		layoutManagerState?.let { outState.putParcelable(STATE_LAYOUT_MANAGER, it) }
		val adapterState = (recyclerView?.adapter as? AppDetailAdapter)?.saveState()
		adapterState?.let { outState.putParcelable(STATE_ADAPTER, it) }
	}

	private fun updateButtons(preference: ProductPreference = ProductPreferences[packageName]) {
		val installed = installed
		val product = Product.findSuggested(
			products,
			installed?.installedItem
		) { it.first }?.first
		val compatible = product != null && product.selectedReleases.firstOrNull()
			.let { it != null && it.incompatibilities.isEmpty() }
		val canInstall = product != null && installed == null && compatible
		val canUpdate =
			product != null && compatible && product.canUpdate(installed?.installedItem) &&
					!preference.shouldIgnoreUpdate(product.versionCode)
		val canUninstall = product != null && installed != null && !installed.isSystem
		val canLaunch =
			product != null && installed != null && installed.launcherActivities.isNotEmpty()
		val canShare = product != null && (
				products[0].second.name == "F-Droid" || products[0].second.name.contains("IzzyOnDroid")
				)

		val actions = mutableSetOf<Action>()

		if (canInstall) actions += Action.INSTALL
		if (canUpdate) actions += Action.UPDATE
		if (canLaunch) actions += Action.LAUNCH
		if (installed != null) actions += Action.DETAILS
		if (canUninstall) actions += Action.UNINSTALL
		if (canShare) actions += Action.SHARE

		val primaryAction = when {
			canUpdate -> Action.UPDATE
			canLaunch -> Action.LAUNCH
			canInstall -> Action.INSTALL
			installed != null -> Action.DETAILS
			canShare -> Action.SHARE
			else -> null
		}

		val adapterAction = when {
			installing -> null
			downloading -> AppDetailAdapter.Action.CANCEL
			else -> primaryAction?.adapterAction
		}

		(recyclerView?.adapter as? AppDetailAdapter)?.action = adapterAction

		for (action in sequenceOf(
			Action.INSTALL,
			Action.SHARE,
			Action.UPDATE,
			Action.UNINSTALL
		)) {
			toolbar.menu.findItem(action.id).isEnabled = !downloading
		}
		this.actions = Pair(actions, primaryAction)
		updateToolbarButtons()
	}

	private fun updateToolbarButtons() {
		val (actions, primaryAction) = actions
		val showPrimaryAction =
			(recyclerView?.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() != 0
		val displayActions = actions.toMutableSet()
		if (!showPrimaryAction && primaryAction != null) {
			displayActions -= primaryAction
		}
		if (displayActions.size >= 4 && resources.configuration.screenWidthDp < 400) {
			displayActions -= Action.DETAILS
		}
		Action.values().forEach { action ->
			toolbar.menu.findItem(action.id).isVisible = action in displayActions
		}
	}

	private fun updateInstallState(installerState: InstallerQueueState) {
		val status = if (packageName isInstalling installerState) AppDetailAdapter.Status.Installing
		else if (packageName isQueuedIn installerState) AppDetailAdapter.Status.PendingInstall
		else AppDetailAdapter.Status.Idle
		val installing = status != AppDetailAdapter.Status.Idle
		if (this.installing != installing) {
			this.installing = installing
			updateButtons()
		}
		(recyclerView?.adapter as? AppDetailAdapter)?.status = status
	}

	private fun updateDownloadState(state: DownloadService.State) {
		val status = when (state) {
			is DownloadService.State.Pending -> AppDetailAdapter.Status.Pending
			is DownloadService.State.Connecting -> AppDetailAdapter.Status.Connecting
			is DownloadService.State.Downloading -> AppDetailAdapter.Status.Downloading(
				state.read,
				state.total
			)
			else -> AppDetailAdapter.Status.Idle
		}
		val downloading = status != AppDetailAdapter.Status.Idle
		if (this.downloading != downloading) {
			this.downloading = downloading
			updateButtons()
		}
		(recyclerView?.adapter as? AppDetailAdapter)?.status = status
		lifecycleScope.launch {
			if (state is DownloadService.State.Success && isResumed) {
				val installItem = packageName installFrom state.release.cacheFileName
				installer + installItem
			}
		}
	}

	override fun onActionClick(action: AppDetailAdapter.Action) {
		when (action) {
			AppDetailAdapter.Action.INSTALL,
			AppDetailAdapter.Action.UPDATE,
			-> startUpdate(
				packageName,
				installed?.installedItem,
				products,
				downloadConnection
			)
			AppDetailAdapter.Action.LAUNCH -> {
				val launcherActivities = installed?.launcherActivities.orEmpty()
				if (launcherActivities.size >= 2) {
					LaunchDialog(launcherActivities).show(
						childFragmentManager,
						LaunchDialog::class.java.name
					)
				} else {
					launcherActivities.firstOrNull()?.let { startLauncherActivity(it.first) }
				}
				Unit
			}
			AppDetailAdapter.Action.DETAILS -> {
				startActivity(
					Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
						.setData(Uri.parse("package:$packageName"))
				)
			}
			AppDetailAdapter.Action.UNINSTALL -> {
				lifecycleScope.launch {
					installer - packageName.toPackageName()
				}
				Unit
			}
			AppDetailAdapter.Action.CANCEL -> {
				val binder = downloadConnection.binder
				if (downloading && binder != null) {
					binder.cancel(packageName)
				} else Unit
			}
			AppDetailAdapter.Action.SHARE -> {
				val address = if (products[0].second.name == "F-Droid") {
					"https://www.f-droid.org/packages/${products[0].first.packageName}/"
				} else if (products[0].second.name.contains("IzzyOnDroid")) {
					"https://apt.izzysoft.de/fdroid/index/apk/${products[0].first.packageName}"
				} else toString()
				val sendIntent: Intent = Intent().apply {
					this.action = Intent.ACTION_SEND
					putExtra(Intent.EXTRA_TEXT, address)
					type = "text/plain"
				}
				startActivity(Intent.createChooser(sendIntent, null))
			}
		}::class
	}

	override fun onFavouriteClicked() {
		lifecycleScope.launch {
			userPreferencesRepository.toggleFavourites(packageName)
		}
	}

	private fun startLauncherActivity(name: String) {
		try {
			startActivity(
				Intent(Intent.ACTION_MAIN)
					.addCategory(Intent.CATEGORY_LAUNCHER)
					.setComponent(ComponentName(packageName, name))
					.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	override fun onPreferenceChanged(preference: ProductPreference) {
		updateButtons(preference)
	}

	override fun onPermissionsClick(group: String?, permissions: List<String>) {
		MessageDialog(MessageDialog.Message.Permissions(group, permissions))
			.show(childFragmentManager)
	}

	override fun onScreenshotClick(screenshot: Product.Screenshot) {
		val pair = products.asSequence()
			.map { it ->
				Pair(
					it.second,
					it.first.screenshots.find { it === screenshot }?.identifier
				)
			}
			.filter { it.second != null }.firstOrNull()
		if (pair != null) {
			val (repository, identifier) = pair
			if (identifier != null) {
				ScreenshotsFragment(packageName, repository.id, identifier)
					.show(childFragmentManager)
			}
		}
	}

	override fun onReleaseClick(release: Release) {
		val installedItem = installed?.installedItem
		when {
			release.incompatibilities.isNotEmpty() -> {
				MessageDialog(
					MessageDialog.Message.ReleaseIncompatible(
						release.incompatibilities,
						release.platforms, release.minSdkVersion, release.maxSdkVersion
					)
				).show(childFragmentManager)
			}
			installedItem != null && installedItem.versionCode > release.versionCode -> {
				MessageDialog(MessageDialog.Message.ReleaseOlder).show(childFragmentManager)
			}
			installedItem != null && installedItem.signature != release.signature -> {
				MessageDialog(MessageDialog.Message.ReleaseSignatureMismatch).show(
					childFragmentManager
				)
			}
			else -> {
				val productRepository =
					products.asSequence().filter { it -> it.first.releases.any { it === release } }
						.firstOrNull()
				if (productRepository != null) {
					downloadConnection.binder?.enqueue(
						packageName, productRepository.first.name,
						productRepository.second, release, installedItem != null
					)
				}
			}
		}
	}

	override fun onUriClick(uri: Uri, shouldConfirm: Boolean): Boolean {
		return if (shouldConfirm && (uri.scheme == "http" || uri.scheme == "https")) {
			MessageDialog(MessageDialog.Message.Link(uri)).show(childFragmentManager)
			true
		} else {
			try {
				startActivity(Intent(Intent.ACTION_VIEW, uri))
				true
			} catch (e: ActivityNotFoundException) {
				e.printStackTrace()
				false
			}
		}
	}

	class LaunchDialog() : DialogFragment() {
		companion object {
			private const val EXTRA_NAMES = "names"
			private const val EXTRA_LABELS = "labels"
		}

		constructor(launcherActivities: List<Pair<String, String>>) : this() {
			arguments = Bundle().apply {
				putStringArrayList(EXTRA_NAMES, ArrayList(launcherActivities.map { it.first }))
				putStringArrayList(EXTRA_LABELS, ArrayList(launcherActivities.map { it.second }))
			}
		}

		override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
			val names = requireArguments().getStringArrayList(EXTRA_NAMES)!!
			val labels = requireArguments().getStringArrayList(EXTRA_LABELS)!!
			return MaterialAlertDialogBuilder(requireContext())
				.setTitle(stringRes.launch)
				.setItems(labels.toTypedArray()) { _, position ->
					(parentFragment as AppDetailFragment)
						.startLauncherActivity(names[position])
				}
				.setNegativeButton(stringRes.cancel, null)
				.create()
		}
	}
}
