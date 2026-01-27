package com.looker.droidify.ui.appDetail

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.allowHardware
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.looker.droidify.content.ProductPreferences
import com.looker.droidify.installer.installers.launchShizuku
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.installer.model.isCancellable
import com.looker.droidify.model.InstalledItem
import com.looker.droidify.model.Product
import com.looker.droidify.model.ProductPreference
import com.looker.droidify.model.Release
import com.looker.droidify.model.Repository
import com.looker.droidify.service.Connection
import com.looker.droidify.service.DownloadService
import com.looker.droidify.ui.Message
import com.looker.droidify.ui.MessageDialog
import com.looker.droidify.ui.ScreenFragment
import com.looker.droidify.ui.appDetail.AppDetailViewModel.Companion.ARG_PACKAGE_NAME
import com.looker.droidify.ui.appDetail.AppDetailViewModel.Companion.ARG_REPO_ADDRESS
import com.looker.droidify.utility.common.cache.Cache
import com.looker.droidify.utility.common.extension.doOnFirstDataCommit
import com.looker.droidify.utility.common.extension.dp
import com.looker.droidify.utility.common.extension.getMutatedIcon
import com.looker.droidify.utility.common.extension.isFirstItemVisible
import com.looker.droidify.utility.common.extension.systemBarsPadding
import com.looker.droidify.utility.common.extension.updateAsMutable
import com.looker.droidify.utility.common.shareUrl
import com.looker.droidify.utility.extension.mainActivity
import com.looker.droidify.utility.extension.startUpdate
import com.stfalcon.imageviewer.StfalconImageViewer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.looker.droidify.R.string as stringRes

@AndroidEntryPoint
class AppDetailFragment() : ScreenFragment(), AppDetailAdapter.Callbacks {
    companion object {
        private const val STATE_ADAPTER = "adapter"
    }

    constructor(packageName: String, repoAddress: String? = null) : this() {
        arguments = Bundle(2).apply {
            putString(ARG_PACKAGE_NAME, packageName)
            putString(ARG_REPO_ADDRESS, repoAddress)
        }
    }

    private enum class Action(
        val id: Int,
        val adapterAction: AppDetailAdapter.Action,
    ) {
        INSTALL(1, AppDetailAdapter.Action.INSTALL),
        UPDATE(2, AppDetailAdapter.Action.UPDATE),
        LAUNCH(3, AppDetailAdapter.Action.LAUNCH),
        DETAILS(4, AppDetailAdapter.Action.DETAILS),
        UNINSTALL(5, AppDetailAdapter.Action.UNINSTALL),
        SOURCE(6, AppDetailAdapter.Action.SOURCE),
        SHARE(7, AppDetailAdapter.Action.SHARE),
    }

    private val viewModel: AppDetailViewModel by viewModels()

    @SuppressLint("RestrictedApi")
    private var layoutManagerState: LinearLayoutManager.SavedState? = null

    private var actions: Pair<Set<Action>, Action?> = Pair(emptySet(), null)
    private var products: List<Pair<Product, Repository>> = emptyList()
    private var installed: Installed? = null
    private var downloading: Boolean = false
    private var installing: InstallState? = null

    private var recyclerView: RecyclerView? = null
    private var detailAdapter: AppDetailAdapter? = null
    private var imageViewer: StfalconImageViewer.Builder<Product.Screenshot>? = null

    private val downloadConnection = Connection(
        serviceClass = DownloadService::class.java,
        onBind = { _, binder ->
            lifecycleScope.launch {
                binder.downloadState.collect(::updateDownloadState)
            }
        },
    )

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel = viewModel

        val detailAdapter = AppDetailAdapter(
            coroutineScope = viewLifecycleOwner.lifecycleScope,
            defaultDispatcher = Dispatchers.Default,
            callbacks = this,
        )
        this.detailAdapter = detailAdapter

        mainActivity.onToolbarCreated(toolbar)
        toolbar.menu.run {
            val context = toolbar.context
            Action.entries.forEach { action ->
                val adapterAction = action.adapterAction
                add(0, action.id, 0, adapterAction.titleResId)
                    .setIcon(context.getMutatedIcon(adapterAction.iconResId))
                    .setVisible(false)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    .setOnMenuItemClickListener {
                        onActionClick(adapterAction)
                        true
                    }
            }
        }

        val content = fragmentBinding.fragmentContent

        val recyclerView = RecyclerView(content.context).apply {
            id = android.R.id.list
            layoutManager = LinearLayoutManager(
                /* context = */ context,
                /* orientation = */ LinearLayoutManager.VERTICAL,
                /* reverseLayout = */ false,
            )
            isMotionEventSplittingEnabled = false
            isVerticalScrollBarEnabled = false
            adapter = detailAdapter
            setHasFixedSize(true)

            itemAnimator = null

            if (savedInstanceState != null) {
                BundleCompat.getParcelable(
                    /* in = */ savedInstanceState,
                    /* key = */ STATE_ADAPTER,
                    /* clazz = */ AppDetailAdapter.SavedState::class.java,
                )?.let(detailAdapter::restoreState)
            }
            clipToPadding = false
            updatePadding(bottom = 24.dp)
            systemBarsPadding(includeFab = false)
        }
        this.recyclerView = recyclerView

        content.addView(recyclerView)

        enableRecyclerViewAnimationsAfterFirstUpdate(
            recyclerView = recyclerView
        )

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.state.collectLatest { state ->
                        products = state.products
                        layoutManagerState = null
                        installed = state.installed
                    }
                }
                launch {
                    viewModel.appDetailListState.collect {
                        detailAdapter.setState(it)

                        // `delay` is cancellable hence it waits for 50 milliseconds to show empty page
                        if (it.items.isEmpty()) delay(50)
                        updateButtons()
                    }
                }
                launch {
                    viewModel.installerState.collect(::updateInstallState)
                }
                launch {
                    recyclerView.isFirstItemVisible.collect(::updateToolbarButtons)
                }
            }
        }

        downloadConnection.bind(requireContext())
    }

    private fun enableRecyclerViewAnimationsAfterFirstUpdate(
        recyclerView: RecyclerView,
    ) {
        recyclerView.doOnFirstDataCommit {
            it.postOnAnimation(object : Runnable {
                private var i = 0

                override fun run() {
                    if (i < 2) {
                        i++
                        it.postOnAnimation(this)
                    } else {
                        it.itemAnimator = DefaultItemAnimator()
                    }
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView = null
        detailAdapter = null
        imageViewer = null

        downloadConnection.unbind(requireContext())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val detailAdapter = detailAdapter
        if (detailAdapter != null) {
            outState.putParcelable(STATE_ADAPTER, detailAdapter.saveState())
        }
    }

    private fun updateButtons(
        preference: ProductPreference = ProductPreferences[viewModel.packageName],
    ) {
        val installed = installed
        val installedItem = installed?.installedItem
        val product = products.findSuggested(installedItem)?.first
        val compatible = product != null && product.compatible
        val canInstall = product != null && installed == null && compatible
        val canUpdate = product != null && compatible && product.canUpdate(installedItem)
            && !preference.shouldIgnoreUpdate(product.versionCode)
        val canUninstall = product != null && installed != null && !installed.isSystem
        val canLaunch = product != null && installed != null && installed.launcherActivities.isNotEmpty()

        val actions = buildSet {
            if (canInstall) add(Action.INSTALL)
            if (canUpdate) add(Action.UPDATE)
            if (canLaunch) add(Action.LAUNCH)
            if (installed != null) add(Action.DETAILS)
            if (canUninstall) add(Action.UNINSTALL)
            add(Action.SHARE)
            add(Action.SOURCE)
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

        detailAdapter?.action = adapterAction

        toolbar.menu.run {
            findItem(Action.INSTALL.id).isEnabled = !downloading
            findItem(Action.UPDATE.id).isEnabled = !downloading
        }

        this.actions = Pair(actions, primaryAction)
        updateToolbarButtons()
    }

    private fun updateToolbarButtons(
        isActionVisible: Boolean = (recyclerView?.layoutManager as LinearLayoutManager)
            .findFirstVisibleItemPosition() == 0,
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
        detailAdapter?.status = status
        installing = installerState
        updateButtons()
    }

    private fun updateDownloadState(state: DownloadService.DownloadState) {
        val detailAdapter = detailAdapter

        val currentItem = state.currentItem
        val packageName = viewModel.packageName
        val isPending = packageName in state.queue
        val isDownloading = state isDownloading packageName
        val isCompleted = state isComplete packageName
        val isActive = isPending || isDownloading

        detailAdapter?.status = when {
            isPending -> {
                AppDetailAdapter.Status.Pending
            }
            isDownloading -> {
                when (currentItem) {
                    is DownloadService.State.Connecting -> AppDetailAdapter.Status.Connecting
                    is DownloadService.State.Downloading -> AppDetailAdapter.Status.Downloading(
                        currentItem.read,
                        currentItem.total,
                    )

                    else -> AppDetailAdapter.Status.Idle
                }
            }
            isCompleted -> {
                AppDetailAdapter.Status.Idle
            }
            else -> {
                detailAdapter.status
            }
        }

        if (this.downloading != isActive) {
            this.downloading = isActive
            updateButtons()
        }

        if (currentItem is DownloadService.State.Success && isResumed) {
            viewModel.installPackage(
                currentItem.packageName,
                currentItem.release.cacheFileName,
            )
        }
    }

    override fun onActionClick(action: AppDetailAdapter.Action) {
        when (action) {
            AppDetailAdapter.Action.INSTALL,
            AppDetailAdapter.Action.UPDATE,
                -> {
                val context = requireContext()
                if (Cache.getEmptySpace(context) < products.first().first.releases.first().size) {
                    MessageDialog(Message.InsufficientStorage).show(childFragmentManager)
                    return
                }
                val shizukuState = viewModel.shizukuState(context)
                if (shizukuState != null && shizukuState.check) {
                    shizukuDialog(
                        context = context,
                        shizukuState = shizukuState,
                        openShizuku = { launchShizuku(context) },
                        switchInstaller = { viewModel.setDefaultInstaller() },
                    ).show()
                    return
                }
                downloadConnection.startUpdate(
                    packageName = viewModel.packageName,
                    installedItem = installed?.installedItem,
                    products = products,
                )
            }

            AppDetailAdapter.Action.LAUNCH -> {
                val launcherActivities = installed?.launcherActivities.orEmpty()
                when {
                    launcherActivities.size >= 2 -> {
                        LaunchDialog(launcherActivities).show(
                            childFragmentManager,
                            LaunchDialog::class.java.name,
                        )
                    }
                    launcherActivities.size == 1 -> {
                        startLauncherActivity(launcherActivities.first().first)
                    }
                }
            }

            AppDetailAdapter.Action.DETAILS -> {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        "package:${viewModel.packageName}".toUri(),
                    ),
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
                val packageName = viewModel.packageName
                val repo = products[0].second
                val address = when {
                    "https://f-droid.org/repo" in repo.mirrors ->
                        "https://f-droid.org/packages/$packageName/"

                    "https://f-droid.org/archive/repo" in repo.mirrors ->
                        "https://f-droid.org/packages/$packageName/"

                    "https://apt.izzysoft.de/fdroid/repo" in repo.mirrors ->
                        "https://apt.izzysoft.de/fdroid/index/apk/$packageName"

                    else -> shareUrl(packageName, repo.address)
                }
                val sendIntent = Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_TEXT, address)
                    .setType("text/plain")
                startActivity(Intent.createChooser(sendIntent, null))
            }

            AppDetailAdapter.Action.SOURCE -> {
                val link = products[0].first.source
                startActivity(Intent(Intent.ACTION_VIEW, link.toUri()))
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
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
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

    override fun onScreenshotClick(item: ScreenshotItem) {
        val productRepository = products.findSuggested(installed?.installedItem) ?: return
        val screenshots = productRepository.first.screenshots.filter {
            it.type != Product.Screenshot.Type.VIDEO
        }

        val position = screenshots.indexOfFirst { it == item.screenshot }
            .coerceAtLeast(0)

        val iv = imageViewer ?: StfalconImageViewer.Builder(
            requireContext(),
            screenshots,
        ) { view, current ->
            val screenshotUrl = current.url(
                context = view.context!!,
                repository = productRepository.second,
                packageName = viewModel.packageName,
            )
            view.load(screenshotUrl) {
                allowHardware(false)
            }
        }.also {
            imageViewer = it
        }

        iv.withStartPosition(position)
        iv.show()
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
                        release.maxSdkVersion,
                    ),
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
        val productRepository = products.firstOrNull { (product, _) ->
            product.releases.any { it == release }
        }
        if (productRepository != null) {
            downloadConnection.binder?.enqueue(
                packageName = viewModel.packageName,
                name = productRepository.first.name,
                repository = productRepository.second,
                release = release,
                isUpdate = installedItem != null,
            )
        }
    }

    override fun onRequestAddRepository(address: String) {
        mainActivity.navigateAddRepository(address)
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
            arguments = Bundle(2).apply {
                val size = launcherActivities.size
                putStringArrayList(EXTRA_NAMES, launcherActivities.mapTo(ArrayList(size)) { it.first })
                putStringArrayList(EXTRA_LABELS, launcherActivities.mapTo(ArrayList(size)) { it.second })
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
            val args = requireArguments()
            val names = args.getStringArrayList(EXTRA_NAMES)!!
            val labels = args.getStringArrayList(EXTRA_LABELS)!!
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
