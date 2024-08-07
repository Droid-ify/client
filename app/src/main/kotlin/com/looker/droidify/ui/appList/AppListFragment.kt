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
import com.looker.core.common.Scroller
import com.looker.core.common.R as CommonR
import com.looker.core.common.R.string as stringRes
import com.looker.core.common.extension.dp
import com.looker.core.common.extension.isFirstItemVisible
import com.looker.core.common.extension.systemBarsMargin
import com.looker.core.common.extension.systemBarsPadding
import com.looker.droidify.model.ProductItem
import com.looker.droidify.database.CursorOwner
import com.looker.droidify.databinding.RecyclerViewWithFabBinding
import com.looker.droidify.utility.extension.screenActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        val order: Boolean,
        val updateAll: Boolean
    ) {
        AVAILABLE(stringRes.available, true, true, false),
        INSTALLED(stringRes.installed, false, true, false),
        UPDATES(stringRes.updates, false, false, true)
    }

    constructor(source: Source) : this() {
        arguments = Bundle().apply {
            putString(EXTRA_SOURCE, source.name)
        }
    }

    val source: Source
        get() = requireArguments().getString(EXTRA_SOURCE)!!.let(Source::valueOf)

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: AppListAdapter
    private var shortAnimationDuration: Int = 0
    private var layoutManagerState: Parcelable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = RecyclerViewWithFabBinding.inflate(inflater, container, false)

        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

        viewModel.syncConnection.bind(requireContext())

        recyclerView = binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            isMotionEventSplittingEnabled = false
            setHasFixedSize(true)
            recycledViewPool.setMaxRecycledViews(AppListAdapter.ViewType.PRODUCT.ordinal, 30)
            recyclerViewAdapter = AppListAdapter(source) {
                screenActivity.navigateProduct(it.packageName)
            }
            adapter = recyclerViewAdapter
            systemBarsPadding()
        }
        val fab = binding.scrollUp
        with(fab) {
            if (source.updateAll) {
                text = getString(CommonR.string.update_all)
                setOnClickListener { viewModel.updateAll() }
                setIconResource(CommonR.drawable.ic_download)
                alpha = 1f
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.showUpdateAllButton.collect {
                        isVisible = it
                    }
                }
                systemBarsMargin(16.dp)
            } else {
                text = ""
                setIconResource(CommonR.drawable.arrow_up)
                setOnClickListener {
                    val scroller = Scroller(requireContext())
                    scroller.targetPosition = 0
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

        updateRequest()
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    viewModel.reposStream.collect { repos ->
                        recyclerViewAdapter.repositories = repos.associateBy { it.id }
                    }
                }
                launch {
                    viewModel.sortOrderFlow.collect {
                        updateRequest()
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        (layoutManagerState ?: recyclerView.layoutManager?.onSaveInstanceState())
            ?.let { outState.putParcelable(STATE_LAYOUT_MANAGER, it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.syncConnection.unbind(requireContext())
        _binding = null
        screenActivity.cursorOwner.detach(this)
    }

    override fun onCursorData(request: CursorOwner.Request, cursor: Cursor?) {
        recyclerViewAdapter.cursor = cursor
        recyclerViewAdapter.emptyText = when {
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

    internal fun setSearchQuery(searchQuery: String) {
        viewModel.setSearchQuery(searchQuery) {
            updateRequest()
        }
    }

    internal fun setSection(section: ProductItem.Section) {
        viewModel.setSection(section) {
            updateRequest()
        }
    }

    private fun updateRequest() {
        if (view != null) {
            screenActivity.cursorOwner.attach(this, viewModel.request(source))
        }
    }
}
