package com.looker.droidify.ui.fragments

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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.looker.droidify.R
import com.looker.droidify.content.ProductPreferences
import com.looker.droidify.database.Database
import com.looker.droidify.entity.*
import com.looker.droidify.installer.AppInstaller
import com.looker.droidify.screen.MessageDialog
import com.looker.droidify.screen.ScreenFragment
import com.looker.droidify.screen.ScreenshotsFragment
import com.looker.droidify.service.Connection
import com.looker.droidify.service.DownloadService
import com.looker.droidify.ui.adapters.AppDetailAdapter
import com.looker.droidify.utility.RxUtils
import com.looker.droidify.utility.Utils
import com.looker.droidify.utility.Utils.startUpdate
import com.looker.droidify.utility.extension.android.*
import com.looker.droidify.utility.extension.text.trimAfter
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private class Nullable<T>(val value: T?)

    private enum class Action(
        val id: Int,
        val adapterAction: AppDetailAdapter.Action,
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
        val launcherActivities: List<Pair<String, String>>,
    )

    val packageName: String
        get() = requireArguments().getString(EXTRA_PACKAGE_NAME)!!

    private var layoutManagerState: LinearLayoutManager.SavedState? = null

    private var actions = Pair(emptySet<Action>(), null as Action?)
    private var products = emptyList<Pair<Product, Repository>>()
    private var installed: Installed? = null
    private var downloading = false

    private var recyclerView: RecyclerView? = null

    private var productDisposable: Disposable? = null
    private val downloadConnection = Connection(DownloadService::class.java, onBind = { _, binder ->
        lifecycleScope.launch {
            binder.stateSubject.filter { it.packageName == packageName }.collect {
                updateDownloadState(it)
            }
        }
    })

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
            addOnScrollListener(scrollListener)
            savedInstanceState?.getParcelable<AppDetailAdapter.SavedState>(STATE_ADAPTER)
                ?.let(adapter::restoreState)
            layoutManagerState = savedInstanceState?.getParcelable(STATE_LAYOUT_MANAGER)
            recyclerView = this
        })

        var first = true
        productDisposable = Observable.just(Unit)
            .concatWith(Database.observable(Database.Subject.Products))
            .observeOn(Schedulers.io())
            .flatMapSingle { RxUtils.querySingle { Database.ProductAdapter.get(packageName, it) } }
            .flatMapSingle { products ->
                RxUtils
                    .querySingle { Database.RepositoryAdapter.getAll(it) }
                    .map { it ->
                        it.asSequence().map { Pair(it.id, it) }.toMap()
                            .let {
                                products.mapNotNull { product ->
                                    it[product.repositoryId]?.let {
                                        Pair(
                                            product,
                                            it
                                        )
                                    }
                                }
                            }
                    }
            }
            .flatMapSingle { products ->
                RxUtils
                    .querySingle { Nullable(Database.InstalledAdapter.get(packageName, it)) }
                    .map { Pair(products, it) }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { it ->
                val (products, installedItem) = it
                val firstChanged = first
                first = false
                val productChanged = this.products != products
                val installedItemChanged = this.installed?.installedItem != installedItem.value
                if (firstChanged || productChanged || installedItemChanged) {
                    layoutManagerState?.let {
                        recyclerView?.layoutManager!!.onRestoreInstanceState(
                            it
                        )
                    }
                    layoutManagerState = null
                    if (firstChanged || productChanged) {
                        this.products = products
                    }
                    if (firstChanged || installedItemChanged) {
                        installed = installedItem.value?.let {
                            val isSystem = try {
                                ((requireContext().packageManager.getApplicationInfo(
                                    packageName,
                                    0
                                ).flags)
                                        and ApplicationInfo.FLAG_SYSTEM) != 0
                            } catch (e: Exception) {
                                false
                            }
                            val launcherActivities =
                                if (packageName == requireContext().packageName) {
                                    // Don't allow to launch self
                                    emptyList()
                                } else {
                                    val packageManager = requireContext().packageManager
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
                                                activityInfo.loadLabel(packageManager).toString()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                null
                                            }
                                            label?.let { labelName ->
                                                Pair(
                                                    activityInfo.name,
                                                    labelName
                                                )
                                            }
                                        }
                                        .toList()
                                }
                            Installed(it, isSystem, launcherActivities)
                        }
                    }
                    val recyclerView = recyclerView!!
                    val adapter = recyclerView.adapter as AppDetailAdapter
                    if (firstChanged || productChanged || installedItemChanged) {
                        adapter.setProducts(
                            recyclerView.context,
                            packageName,
                            products,
                            installedItem.value
                        )
                    }
                    lifecycleScope.launch { updateButtons() }
                }
            }

        downloadConnection.bind(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView = null

        productDisposable?.dispose()
        productDisposable = null
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

    private suspend fun updateButtons() {
        updateButtons(ProductPreferences[packageName])
    }

    private suspend fun updateButtons(preference: ProductPreference) {
        val installed = installed
        val product = Product.findSuggested(products, installed?.installedItem) { it.first }?.first
        val compatible = product != null && product.selectedReleases.firstOrNull()
            .let { it != null && it.incompatibilities.isEmpty() }
        val canInstall = product != null && installed == null && compatible
        val canUpdate =
            product != null && compatible && product.canUpdate(installed?.installedItem) &&
                    !preference.shouldIgnoreUpdate(product.versionCode)
        val canUninstall = product != null && installed != null && !installed.isSystem
        val canLaunch =
            product != null && installed != null && installed.launcherActivities.isNotEmpty()
        val canShare = product != null && products[0].second.name == "F-Droid"

        val actions = mutableSetOf<Action>()
        if (canInstall) {
            actions += Action.INSTALL
        }
        if (canUpdate) {
            actions += Action.UPDATE
        }
        if (canLaunch) {
            actions += Action.LAUNCH
        }
        if (installed != null) {
            actions += Action.DETAILS
        }
        if (canUninstall) {
            actions += Action.UNINSTALL
        }
        if (canShare) {
            actions += Action.SHARE
        }
        val primaryAction = when {
            canUpdate -> Action.UPDATE
            canLaunch -> Action.LAUNCH
            canInstall -> Action.INSTALL
            installed != null -> Action.DETAILS
            canShare -> Action.SHARE
            else -> null
        }

        val adapterAction =
            if (downloading) AppDetailAdapter.Action.CANCEL else primaryAction?.adapterAction
        (recyclerView?.adapter as? AppDetailAdapter)?.setAction(adapterAction)

        for (action in sequenceOf(
            Action.INSTALL,
            Action.SHARE,
            Action.UPDATE,
            Action.UNINSTALL
        )) {
            toolbar.menu.findItem(action.id).isEnabled = !downloading
        }
        this.actions = Pair(actions, primaryAction)
        withContext(Dispatchers.Main) { updateToolbarButtons() }
    }

    private suspend fun updateToolbarTitle() {
        withContext(Dispatchers.Default) {
            val showPackageName = recyclerView
                ?.let { (it.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() != 0 } == true
            collapsingToolbar.title =
                if (showPackageName) products[0].first.name.trimAfter(' ', 2)
                else getString(R.string.application)
        }
    }

    private suspend fun updateToolbarButtons() {
        withContext(Dispatchers.Default) {
            val (actions, primaryAction) = actions
            val showPrimaryAction = recyclerView
                ?.let { (it.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() != 0 } == true
            val displayActions = actions.toMutableSet()
            if (!showPrimaryAction && primaryAction != null) {
                displayActions -= primaryAction
            }
            if (displayActions.size >= 4 && resources.configuration.screenWidthDp < 400) {
                displayActions -= Action.DETAILS
            }

            launch(Dispatchers.Main) {
                for (action in Action.values())
                    toolbar.menu.findItem(action.id).isVisible = action in displayActions
            }
        }
    }

    private suspend fun updateDownloadState(state: DownloadService.State?) {
        val status = when (state) {
            is DownloadService.State.Pending -> AppDetailAdapter.Status.Pending
            is DownloadService.State.Connecting -> AppDetailAdapter.Status.Connecting
            is DownloadService.State.Downloading -> AppDetailAdapter.Status.Downloading(
                state.read,
                state.total
            )
            is DownloadService.State.Success, is DownloadService.State.Error, is DownloadService.State.Cancel, null -> null
        }
        val downloading = status != null
        if (this.downloading != downloading) {
            this.downloading = downloading
            updateButtons()
        }
        (recyclerView?.adapter as? AppDetailAdapter)?.setStatus(status)
        if (state is DownloadService.State.Success && isResumed) {
            withContext(Dispatchers.Default) {
                AppInstaller.getInstance(context)?.defaultInstaller?.install(state.release.cacheFileName)
            }
        }
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        private var lastPosition = -1

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val position =
                (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            val lastPosition = lastPosition
            this.lastPosition = position
            if ((lastPosition == 0) != (position == 0)) {
                lifecycleScope.launch {
                    launch { updateToolbarTitle() }
                    launch { updateToolbarButtons() }
                }
            }
        }
    }

    override fun onActionClick(action: AppDetailAdapter.Action) {
        when (action) {
            AppDetailAdapter.Action.INSTALL,
            AppDetailAdapter.Action.UPDATE,
            -> {
                val installedItem = installed?.installedItem
                lifecycleScope.launch {
                    startUpdate(
                        packageName,
                        installedItem,
                        products,
                        downloadConnection
                    )
                }
                Unit
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
                    AppInstaller.getInstance(context)?.defaultInstaller?.uninstall(packageName)
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
                val sendIntent: Intent = Intent().apply {
                    this.action = Intent.ACTION_SEND
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "https://www.f-droid.org/packages/${products[0].first.packageName}/"
                    )
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(sendIntent, null))
            }
        }::class
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
        lifecycleScope.launch { updateButtons(preference) }
    }

    override fun onPermissionsClick(group: String?, permissions: List<String>) {
        MessageDialog(MessageDialog.Message.Permissions(group, permissions)).show(
            childFragmentManager
        )
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
                ScreenshotsFragment(packageName, repository.id, identifier).show(
                    childFragmentManager
                )
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
                        productRepository.second, release
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
                .setTitle(R.string.launch)
                .setItems(labels.toTypedArray()) { _, position ->
                    (parentFragment as AppDetailFragment)
                        .startLauncherActivity(names[position])
                }
                .setNegativeButton(R.string.cancel, null)
                .create()
        }
    }
}
