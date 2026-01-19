package com.looker.droidify.ui.appList

import android.database.Cursor
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.looker.droidify.R
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.database.CursorOwner
import com.looker.droidify.databinding.RecyclerViewWithFabBinding
import com.looker.droidify.installer.model.InstallState
import com.looker.droidify.model.ProductItem
import com.looker.droidify.service.Connection
import com.looker.droidify.service.DownloadService
import com.looker.droidify.ui.DownloadStatus
import com.looker.droidify.utility.common.Scroller
import com.looker.droidify.utility.common.extension.dp
import com.looker.droidify.utility.common.extension.isFirstItemVisible
import com.looker.droidify.utility.common.extension.systemBarsMargin
import com.looker.droidify.utility.common.extension.systemBarsPadding
import com.looker.droidify.utility.extension.mainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import com.looker.droidify.R.string as stringRes

@AndroidEntryPoint
class AppListFragment() : Fragment(), CursorOwner.Callback {

    private val viewModel: AppListViewModel by viewModels()

    private var _binding: RecyclerViewWithFabBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val STATE_LAYOUT_MANAGER = "layoutManager"

        private const val EXTRA_SOURCE = "source"
    }

    enum class Source(
        val titleResId: Int,
        val sections: Boolean,
        val updateAll: Boolean,
    ) {
        AVAILABLE(stringRes.available, true, false),
        INSTALLED(stringRes.installed, false, false),
        UPDATES(stringRes.updates, false, true)
    }

    constructor(source: Source) : this() {
        arguments = Bundle().apply {
            putString(EXTRA_SOURCE, source.name)
        }
    }

    val source: Source
        get() = requireArguments().getString(EXTRA_SOURCE)!!.let(Source::valueOf)

    private lateinit var recyclerView: RecyclerView
    private lateinit var appListAdapter: AppListAdapter
    private var scroller: Scroller? = null
    private var shortAnimationDuration: Int = 0
    private var layoutManagerState: Parcelable? = null

    /**
     * Tracks packages currently showing download progress.
     *
     * Used to detect when packages are no longer in the download queue so their
     * status can be explicitly cleared. Without this, stale statuses could persist
     * if a package is removed from the queue without going through normal completion.
     */
    private val activeDownloadPackages = mutableSetOf<String>()

    /**
     * Tracks packages currently showing install progress.
     *
     * Used to detect when packages are no longer being installed so their
     * status can be explicitly cleared. This handles edge cases where
     * [InstallManager] state changes without explicit completion signals.
     */
    private val activeInstallPackages = mutableSetOf<String>()

    /**
     * Connection to [DownloadService] for observing download progress.
     *
     * On bind:
     * 1. Immediately processes current state (important for showing status on fragment resume)
     * 2. Samples subsequent updates at 200ms to avoid excessive UI updates during active downloads
     */
    private val downloadConnection = Connection(
        serviceClass = DownloadService::class.java,
        onBind = { _, binder ->
            viewLifecycleOwner.lifecycleScope.launch {
                // Process initial state immediately to show status on fragment resume
                updateDownloadState(binder.downloadState.value)
                // Sample subsequent updates to avoid excessive UI updates during downloads
                binder.downloadState
                    .sample(200)
                    .collect { downloadState ->
                        updateDownloadState(downloadState)
                    }
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = RecyclerViewWithFabBinding.inflate(inflater, container, false)

        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

        viewModel.syncConnection.bind(requireContext())
        downloadConnection.bind(requireContext())

        recyclerView = binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            isMotionEventSplittingEnabled = false
            setHasFixedSize(false)
            recycledViewPool.setMaxRecycledViews(AppListAdapter.ViewType.PRODUCT.ordinal, 30)
            appListAdapter = AppListAdapter(source, mainActivity::navigateProduct)
            adapter = appListAdapter
            systemBarsPadding()
        }
        val fab = binding.scrollUp
        with(fab) {
            if (source.updateAll) {
                text = getString(R.string.update_all)
                setOnClickListener { viewModel.updateAll() }
                setIconResource(R.drawable.ic_download)
                alpha = 1f
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.showUpdateAllButton.collect {
                        isVisible = it
                    }
                }
                systemBarsMargin(16.dp)
            } else {
                text = null
                setIconResource(R.drawable.arrow_up)
                setOnClickListener {
                    if (scroller == null) {
                        scroller = Scroller(requireContext())
                    }
                    scroller!!.targetPosition = 0
                    recyclerView.layoutManager?.startSmoothScroll(scroller)
                }
                alpha = 0f
                isVisible = true
                systemBarsMargin(16.dp)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            if (!source.updateAll) {
                recyclerView.isFirstItemVisible.collect { showFab ->
                    fab.animate()
                        .alpha(if (!showFab) 1f else 0f)
                        .setDuration(shortAnimationDuration.toLong())
                        .setListener(null)
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        layoutManagerState = savedInstanceState?.getParcelable(STATE_LAYOUT_MANAGER)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    viewModel.reposStream.collect { repos ->
                        appListAdapter.updateRepos(repos)
                    }
                }
                launch {
                    viewModel.state.collect {
                        updateRequest()
                    }
                }
                // Collect install state updates
                launch {
                    viewModel.installStates.collect { installStates ->
                        updateInstallStates(installStates)
                    }
                }
            }
        }
    }

    /**
     * Processes download state changes from [DownloadService].
     *
     * Maps [DownloadService.State] to [DownloadStatus] and updates the adapter:
     * - Current item: Connecting/Downloading states, or Idle for completed/failed
     * - Queued items: All marked as [DownloadStatus.Pending]
     *
     * Also handles cleanup of packages no longer in the download state by comparing
     * against [activeDownloadPackages] from the previous update.
     *
     * @param downloadState Current state containing the active download and queue
     */
    private fun updateDownloadState(downloadState: DownloadService.DownloadState) {
        val currentItem = downloadState.currentItem
        val packageName = currentItem.packageName

        // Collect all active packages in this update for cleanup comparison
        val currentActivePackages = mutableSetOf<String>()

        // Map DownloadService.State to DownloadStatus
        val status = when (currentItem) {
            is DownloadService.State.Idle -> DownloadStatus.Idle
            is DownloadService.State.Connecting -> DownloadStatus.Connecting
            is DownloadService.State.Downloading -> DownloadStatus.Downloading(
                currentItem.read,
                currentItem.total
            )
            // Completed states (Success/Error/Cancel) map to Idle - install state will take over
            is DownloadService.State.Success,
            is DownloadService.State.Error,
            is DownloadService.State.Cancel -> DownloadStatus.Idle
        }

        if (packageName.isNotEmpty()) {
            appListAdapter.updateDownloadStatus(packageName, status)
            if (status != DownloadStatus.Idle) {
                currentActivePackages.add(packageName)
            }
        }

        // Mark all queued packages as Pending (waiting to download)
        downloadState.queue.forEach { queuedPackage ->
            if (queuedPackage.isNotEmpty() && queuedPackage != packageName) {
                appListAdapter.updateDownloadStatus(queuedPackage, DownloadStatus.Pending)
                currentActivePackages.add(queuedPackage)
            }
        }

        // Clean up packages that were active in previous update but are no longer
        val packagesToClean = activeDownloadPackages - currentActivePackages
        packagesToClean.forEach { pkg ->
            appListAdapter.updateDownloadStatus(pkg, DownloadStatus.Idle)
        }
        activeDownloadPackages.clear()
        activeDownloadPackages.addAll(currentActivePackages)
    }

    /**
     * Processes install state changes from [InstallManager].
     *
     * Maps [InstallState] to [DownloadStatus] and updates the adapter:
     * - [InstallState.Pending] -> [DownloadStatus.PendingInstall] (waiting for installer)
     * - [InstallState.Installing] -> [DownloadStatus.Installing] (installer running)
     * - [InstallState.Installed]/[InstallState.Failed] -> [DownloadStatus.Idle]
     *
     * Note: [InstallState.Pending] maps to [DownloadStatus.PendingInstall], NOT [DownloadStatus.Pending].
     * This distinction is important: Pending = waiting to download, PendingInstall = waiting to install.
     *
     * Also handles cleanup of packages no longer in the install state.
     *
     * @param installStates Map of package names to their current install state
     */
    private fun updateInstallStates(installStates: Map<PackageName, InstallState>) {
        // Collect all active packages in this update for cleanup comparison
        val currentActivePackages = mutableSetOf<String>()

        installStates.forEach { (packageName, state) ->
            // Map InstallState to DownloadStatus
            val status = when (state) {
                // Pending in install queue (download complete, waiting for installer)
                InstallState.Pending -> DownloadStatus.PendingInstall
                InstallState.Installing -> DownloadStatus.Installing
                // Completed states map to Idle
                InstallState.Installed,
                InstallState.Failed -> DownloadStatus.Idle
            }
            appListAdapter.updateInstallStatus(packageName.name, status)
            if (status != DownloadStatus.Idle) {
                currentActivePackages.add(packageName.name)
            }
        }

        // Clear status for packages that were active but are no longer
        val packagesToClean = activeInstallPackages - currentActivePackages
        packagesToClean.forEach { pkg ->
            appListAdapter.updateInstallStatus(pkg, DownloadStatus.Idle)
        }
        activeInstallPackages.clear()
        activeInstallPackages.addAll(currentActivePackages)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        (layoutManagerState ?: recyclerView.layoutManager?.onSaveInstanceState())
            ?.let { outState.putParcelable(STATE_LAYOUT_MANAGER, it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.syncConnection.unbind(requireContext())
        downloadConnection.unbind(requireContext())
        _binding = null
        scroller = null
        mainActivity.cursorOwner.detach(this)
    }

    override fun onCursorData(request: CursorOwner.Request, cursor: Cursor?) {
        appListAdapter.cursor = cursor
        appListAdapter.emptyText = when {
            cursor == null -> ""
            viewModel.searchQuery.value.isNotEmpty() -> {
                getString(stringRes.no_matching_applications_found)
            }

            else -> when (source) {
                Source.AVAILABLE -> getString(stringRes.no_applications_available)
                Source.INSTALLED -> getString(stringRes.no_applications_installed)
                Source.UPDATES -> getString(stringRes.all_applications_up_to_date)
            }
        }
        layoutManagerState?.let {
            layoutManagerState = null
            recyclerView.layoutManager?.onRestoreInstanceState(it)
        }
    }

    fun setSearchQuery(searchQuery: String) {
        viewModel.setSearchQuery(searchQuery)
    }

    fun setSection(section: ProductItem.Section) {
        viewModel.setSection(section)
    }

    fun updateRequest() {
        if (view != null) {
            mainActivity.cursorOwner.attach(this, viewModel.request(source))
        }
    }
}
