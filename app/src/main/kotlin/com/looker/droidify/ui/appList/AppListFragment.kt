package com.looker.droidify.ui.appList

import android.os.Bundle
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
import com.looker.droidify.database.AppListRowViewType
import com.looker.droidify.databinding.RecyclerViewWithFabBinding
import com.looker.droidify.model.ProductItem
import com.looker.droidify.utility.common.Scroller
import com.looker.droidify.utility.common.extension.dp
import com.looker.droidify.utility.common.extension.isFirstItemVisible
import com.looker.droidify.utility.common.extension.systemBarsMargin
import com.looker.droidify.utility.common.extension.systemBarsPadding
import com.looker.droidify.utility.extension.mainActivity
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.launch
import com.looker.droidify.R.string as stringRes

@AndroidEntryPoint
class AppListFragment() : Fragment() {

    private val viewModel: AppListViewModel by viewModels<AppListViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<AppListViewModel.Factory> { factory ->
                factory.create(source)
            }
        }
    )

    private var _binding: RecyclerViewWithFabBinding? = null
    private val binding get() = _binding!!

    companion object {
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

    private var pendingSearchQuery: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = RecyclerViewWithFabBinding.inflate(inflater, container, false)

        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

        val viewModel = viewModel
        viewModel.syncConnection.bind(requireContext())

        val psq = pendingSearchQuery
        if (psq != null) {
            viewModel.setSearchQuery(psq)
            pendingSearchQuery = null
        }

        recyclerView = binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            isMotionEventSplittingEnabled = false
            setHasFixedSize(true)
            recycledViewPool.setMaxRecycledViews(AppListRowViewType.PRODUCT, 30)
            appListAdapter = AppListAdapter(requireContext()) {
                mainActivity.navigateProduct(it)
            }
            adapter = appListAdapter
            systemBarsPadding()
        }

        val fab = binding.scrollUp
        with(fab) {
            if (source.updateAll) {
                text = getString(stringRes.update_all)
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

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.listFlow.collect {
                    appListAdapter.submitData(it)
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.syncConnection.unbind(requireContext())
        _binding = null
        scroller = null
    }

    fun setSearchQuery(searchQuery: String) {
        if (view != null) {
            viewModel.setSearchQuery(searchQuery)
        } else {
            pendingSearchQuery = searchQuery
        }
    }

    fun setSection(section: ProductItem.Section) {
        viewModel.setSection(section)
    }

    fun updateRequest() {
        if (view != null) {
            viewModel.forceRefresh()
        }
    }
}
