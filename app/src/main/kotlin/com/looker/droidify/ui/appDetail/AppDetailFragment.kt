package com.looker.droidify.ui.appDetail

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.looker.core.common.cache.Cache
import com.looker.core.common.extension.getLauncherActivities
import com.looker.core.common.extension.getMutatedIcon
import com.looker.core.common.extension.isFirstItemVisible
import com.looker.core.common.extension.isSystemApplication
import com.looker.core.common.extension.systemBarsPadding
import com.looker.core.common.extension.updateAsMutable
import com.looker.droidify.content.ProductPreferences
import com.looker.droidify.model.InstalledItem
import com.looker.droidify.model.Product
import com.looker.droidify.model.ProductPreference
import com.looker.droidify.model.Release
import com.looker.droidify.model.Repository
import com.looker.droidify.model.findSuggested
import com.looker.droidify.service.Connection
import com.looker.droidify.service.DownloadService
import com.looker.droidify.ui.Message
import com.looker.droidify.ui.MessageDialog
import com.looker.droidify.ui.ScreenFragment
import com.looker.droidify.ui.appDetail.AppDetailViewModel.Companion.ARG_PACKAGE_NAME
import com.looker.droidify.ui.appDetail.AppDetailViewModel.Companion.ARG_REPO_ADDRESS
import com.looker.droidify.utility.extension.screenActivity
import com.looker.droidify.utility.extension.startUpdate
import com.looker.installer.model.InstallState
import com.looker.installer.model.isCancellable
import com.stfalcon.imageviewer.StfalconImageViewer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.looker.core.common.R.string as stringRes

@AndroidEntryPoint
class AppDetailFragment() : ScreenFragment(), AppDetailAdapter.Callbacks {
    companion object {
        private const val STATE_LAYOUT_MANAGER = "layoutManager"
        private const val STATE_ADAPTER = "adapter"
    }

    constructor(packageName: String, repoAddress: String? = null) : this() {
        arguments = bundleOf(
            ARG_PACKAGE_NAME to packageName,
            ARG_REPO_ADDRESS to repoAddress
        )
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
        val installedItem: InstalledItem,
        val isSystem: Boolean,
        val launcherActivities: List<Pair<String, String>>
    )

    private val viewModel: AppDetailViewModel by viewModels()

    private var layoutManagerState: LinearLayoutManager.SavedState? = null

    private var actions = Pair(emptySet<Action>(), null as Action?)
    private var products = emptyList<Pair<Product, Repository>>()
    private var installed: Installed? = null
    private var downloading = false
    private var installing: InstallState? = null

    private var recyclerView: RecyclerView? = null
    private var detailAdapter: AppDetailAdapter? = null

    private val downloadConnection = Connection(
        serviceClass = DownloadService::class.java,
        onBind = { _, binder ->
            lifecycleScope.launch {
                binder.downloadState.collect(::updateDownloadState)
            }
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        detailAdapter = AppDetailAdapter(this@AppDetailFragment)
        screenActivity.onToolbarCreated(toolbar)
        toolbar.menu.apply {
            Action.entries.forEach { action ->
                add(0, action.id, 0, action.adapterAction.titleResId)
                    .setIcon(toolbar.context.getMutatedIcon(action.adapterAction.iconResId))
                    .setVisible(false)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    .setOnMenuItemClickListener {
                        onActionClick(action.adapterAction)
                        true
                    }
            }
        }

        val content = fragmentBinding.fragmentContent
        content.addView(
            RecyclerView(content.context).apply {
                id = android.R.id.list
                this.layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.VERTICAL,
                    false
                )
                isMotionEventSplittingEnabled = false
                isVerticalScrollBarEnabled = false
                adapter = detailAdapter
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                if (detailAdapter != null) {
                    savedInstanceState?.getParcelable<AppDetailAdapter.SavedState>(STATE_ADAPTER)
                        ?.let(detailAdapter!!::restoreState)
                }
                layoutManagerState = savedInstanceState?.getParcelable(STATE_LAYOUT_MANAGER)
                recyclerView = this
                systemBarsPadding(includeFab = false)
            }
        )
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.state.collectLatest { state ->
                        products = state.products.mapNotNull { product ->
                            val requiredRepo = state.repos.find { it.id == product.repositoryId }
                            requiredRepo?.let { product to it }
                        }
                        layoutManagerState?.let {
                            recyclerView?.layoutManager!!.onRestoreInstanceState(it)
                        }
                        layoutManagerState = null
                        installed = state.installedItem?.let {
                            with(requireContext().packageManager) {
                                val isSystem = isSystemApplication(viewModel.packageName)
                                val launcherActivities = if (state.isSelf) {
                                    emptyList()
                                } else {
                                    getLauncherActivities(viewModel.packageName)
                                }
                                Installed(it, isSystem, launcherActivities)
                            }
                        }
                        val adapter = recyclerView?.adapter as? AppDetailAdapter

                        // `delay` is cancellable hence it waits for 50 milliseconds to show empty page
                        if (products.isEmpty()) delay(50)

                        adapter?.setProducts(
                            context = requireContext(),
                            packageName = viewModel.packageName,
                            suggestedRepo = state.addressIfUnavailable,
                            products = products,
                            installedItem = state.installedItem,
                            isFavourite = state.isFavourite,
                            allowIncompatibleVersion = state.allowIncompatibleVersions
                        )
                        updateButtons()
                    }
                }
                launch {
                    viewModel.installerState.collect(::updateInstallState)
                }
                launch {
                    recyclerView?.isFirstItemVisible?.collect(::updateToolbarButtons)
                }
            }
        }

        downloadConnection.bind(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView = null
        detailAdapter = null

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

    private fun updateButtons(
        preference: ProductPreference = ProductPreferences[viewModel.packageName]
    ) {
        val installed = installed
        val product = products.findSuggested(installed?.installedItem)?.first
        val compatible = product != null && product.selectedReleases.firstOrNull()
            .let { it != null && it.incompatibilities.isEmpty() }
        val canInstall = product != null && installed == null && compatible
        val canUpdate =
            product != null && compatible && product.canUpdate(installed?.installedItem) &&
                !preference.shouldIgnoreUpdate(product.versionCode)
        val canUninstall = product != null && installed != null && !installed.isSystem
        val canLaunch =
            product != null && installed != null && installed.launcherActivities.isNotEmpty()

        val actions = buildSet {
            if (canInstall) add(Action.INSTALL)
            if (canUpdate) add(Action.UPDATE)
            if (canLaunch) add(Action.LAUNCH)
            if (installed != null) add(Action.DETAILS)
            if (canUninstall) add(Action.UNINSTALL)
            add(Action.SHARE)
        }

        val primaryAction = when {
            canUpdate -> Action.UPDATE
            canLaunch -> Action.LAUNCH
            canInstall -> Action.INSTALL
            installed != null -> Action.DETAILS
            else -> Action.SHARE
        }

        val adapterAction = when {
            installing == InstallState.Installing -> null
            installing == InstallState.Pending -> AppDetailAdapter.Action.CANCEL
            downloading -> AppDetailAdapter.Action.CANCEL
            else -> primaryAction.adapterAction
        }

        (recyclerView?.adapter as? AppDetailAdapter)?.action = adapterAction

        for (action in sequenceOf(
            Action.INSTALL,
            Action.UPDATE,
        )) {
            toolbar.menu.findItem(action.id).isEnabled = !downloading
        }
        this.actions = Pair(actions, primaryAction)
        updateToolbarButtons()
    }

    private fun updateToolbarButtons(
        isActionVisible: Boolean = (recyclerView?.layoutManager as LinearLayoutManager)
            .findFirstVisibleItemPosition() == 0
    ) {
        toolbar.title = if (isActionVisible) {
            getString(stringRes.application)
        } else {
            products.firstOrNull()?.first?.name ?: getString(stringRes.application)
        }
        val (actions, primaryAction) = actions
        val displayActions = actions.updateAsMutable {
            if (isActionVisible && primaryAction != null) {
                remove(primaryAction)
            }
            if (size >= 4 && resources.configuration.screenWidthDp < 400) {
                remove(Action.DETAILS)
            }
        }
        Action.entries.forEach { action ->
            toolbar.menu.findItem(action.id).isVisible = action in displayActions
        }
    }

    private fun updateInstallState(installerState: InstallState?) {
        val status = when (installerState) {
            InstallState.Pending -> AppDetailAdapter.Status.PendingInstall
            InstallState.Installing -> AppDetailAdapter.Status.Installing
            else -> AppDetailAdapter.Status.Idle
        }
        (recyclerView?.adapter as? AppDetailAdapter)?.status = status
        installing = installerState
        updateButtons()
    }

    private fun updateDownloadState(state: DownloadService.DownloadState) {
        val packageName = viewModel.packageName
        val isPending = packageName in state.queue
        val isDownloading = state isDownloading packageName
        val isCompleted = state isComplete packageName
        val isActive = isPending || isDownloading
        if (isPending) {
            detailAdapter?.status = AppDetailAdapter.Status.Pending
        }
        if (isDownloading) {
            detailAdapter?.status = when (state.currentItem) {
                is DownloadService.State.Connecting -> AppDetailAdapter.Status.Connecting
                is DownloadService.State.Downloading -> AppDetailAdapter.Status.Downloading(
                    state.currentItem.read,
                    state.currentItem.total
                )

                else -> AppDetailAdapter.Status.Idle
            }
        }
        if (isCompleted) {
            detailAdapter?.status = AppDetailAdapter.Status.Idle
        }
        if (this.downloading != isActive) {
            this.downloading = isActive
            updateButtons()
        }
        if (state.currentItem is DownloadService.State.Success && isResumed) {
            viewModel.installPackage(
                state.currentItem.packageName,
                state.currentItem.release.cacheFileName
            )
        }
    }

    override fun onActionClick(action: AppDetailAdapter.Action) {
        when (action) {
            AppDetailAdapter.Action.INSTALL,
            AppDetailAdapter.Action.UPDATE -> {
                if (Cache.getEmptySpace(requireContext()) < products.first().first.releases.first().size) {
                    MessageDialog(Message.InsufficientStorage).show(childFragmentManager)
                    return
                }
                downloadConnection.startUpdate(
                    viewModel.packageName,
                    installed?.installedItem,
                    products
                )
            }

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
            }

            AppDetailAdapter.Action.DETAILS -> {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        "package:${viewModel.packageName}".toUri()
                    )
                )
            }

            AppDetailAdapter.Action.UNINSTALL -> viewModel.uninstallPackage()

            AppDetailAdapter.Action.CANCEL -> {
                val binder = downloadConnection.binder
                if (installing?.isCancellable == true) {
                    viewModel.removeQueue()
                } else if (downloading && binder != null) {
                    binder.cancel(viewModel.packageName)
                }
            }

            AppDetailAdapter.Action.SHARE -> {
                val repo = products[0].second
                val address = when {
                    repo.name == "F-Droid" ->
                        "https://f-droid.org/packages/" +
                            "${viewModel.packageName}/"

                    "IzzyOnDroid" in repo.name -> {
                        "https://apt.izzysoft.de/fdroid/index/apk/${viewModel.packageName}"
                    }

                    else -> {
                        "https://droidify.eu.org/app/?id=" +
                            "${viewModel.packageName}&repo_address=${repo.address}"
                    }
                }
                val sendIntent = Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_TEXT, address)
                    .setType("text/plain")
                startActivity(Intent.createChooser(sendIntent, null))
            }
        }
    }

    override fun onFavouriteClicked() {
        viewModel.setFavouriteState()
    }

    private fun startLauncherActivity(name: String) {
        try {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(ComponentName(viewModel.packageName, name))
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
        MessageDialog(Message.Permissions(group, permissions))
            .show(childFragmentManager)
    }

    override fun onScreenshotClick(screenshot: Product.Screenshot, parentView: ImageView) {
        val product = products
            .firstOrNull { (product, _) ->
                product.screenshots.find { it === screenshot }?.identifier != null
            }
            ?: return
        val screenshots = product.first.screenshots
        val position = screenshots.indexOfFirst { screenshot.identifier == it.identifier }
        StfalconImageViewer
            .Builder(context, screenshots) { view, current ->
                view.load(current.url(product.second, viewModel.packageName))
            }
            .withStartPosition(position)
            .show()
    }

    override fun onReleaseClick(release: Release) {
        val installedItem = installed?.installedItem
        when {
            release.incompatibilities.isNotEmpty() -> {
                MessageDialog(
                    Message.ReleaseIncompatible(
                        release.incompatibilities,
                        release.platforms,
                        release.minSdkVersion,
                        release.maxSdkVersion
                    )
                ).show(childFragmentManager)
            }

            Cache.getEmptySpace(requireContext()) < release.size -> {
                MessageDialog(Message.InsufficientStorage).show(childFragmentManager)
            }

            installedItem != null && installedItem.versionCode > release.versionCode -> {
                MessageDialog(Message.ReleaseOlder).show(childFragmentManager)
            }

            installedItem != null && installedItem.signature != release.signature -> {
                lifecycleScope.launch {
                    if (viewModel.shouldIgnoreSignature()) {
                        queueReleaseInstall(release, installedItem)
                    } else {
                        MessageDialog(Message.ReleaseSignatureMismatch).show(childFragmentManager)
                    }
                }
            }

            else -> {
                queueReleaseInstall(release, installedItem)
            }
        }
    }

    private fun queueReleaseInstall(release: Release, installedItem: InstalledItem?) {
        val productRepository =
            products.asSequence().filter { (product, _) ->
                product.releases.any { it === release }
            }.firstOrNull()
        if (productRepository != null) {
            downloadConnection.binder?.enqueue(
                viewModel.packageName,
                productRepository.first.name,
                productRepository.second,
                release,
                installedItem != null
            )
        }
    }

    override fun onRequestAddRepository(address: String) {
        screenActivity.navigateAddRepository(address)
    }

    override fun onUriClick(uri: Uri, shouldConfirm: Boolean): Boolean {
        return if (shouldConfirm && (uri.scheme == "http" || uri.scheme == "https")) {
            MessageDialog(Message.Link(uri)).show(childFragmentManager)
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
