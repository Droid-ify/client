package com.looker.droidify.ui.tabsFragment

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.tabs.TabLayoutMediator
import com.looker.core.common.device.Huawei
import com.looker.core.common.extension.dp
import com.looker.core.common.extension.getMutatedIcon
import com.looker.core.common.extension.selectableBackground
import com.looker.core.common.extension.systemBarsPadding
import com.looker.core.common.sdkAbove
import com.looker.core.datastore.extension.sortOrderName
import com.looker.core.datastore.model.SortOrder
import com.looker.droidify.R
import com.looker.droidify.databinding.TabsToolbarBinding
import com.looker.droidify.model.ProductItem
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.ui.ScreenFragment
import com.looker.droidify.ui.appList.AppListFragment
import com.looker.droidify.utility.extension.resources.sizeScaled
import com.looker.droidify.utility.extension.screenActivity
import com.looker.droidify.widget.DividerConfiguration
import com.looker.droidify.widget.FocusSearchView
import com.looker.droidify.widget.StableRecyclerAdapter
import com.looker.droidify.widget.addDivider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import com.looker.core.common.R as CommonR
import com.looker.core.common.R.string as stringRes

@AndroidEntryPoint
class TabsFragment : ScreenFragment() {

    enum class BackAction {
        ProductAll,
        CollapseSearchView,
        HideSections,
        None,
    }

    private var _tabsBinding: TabsToolbarBinding? = null
    private val tabsBinding get() = _tabsBinding!!

    private val viewModel: TabsViewModel by viewModels()

    companion object {
        private const val STATE_SEARCH_FOCUSED = "searchFocused"
        private const val STATE_SEARCH_QUERY = "searchQuery"
        private const val STATE_SHOW_SECTIONS = "showSections"
    }

    private class Layout(view: TabsToolbarBinding) {
        val tabs = view.tabs
        val sectionLayout = view.sectionLayout
        val sectionChange = view.sectionChange
        val sectionName = view.sectionName
        val sectionIcon = view.sectionIcon
    }

    private var favouritesItem: MenuItem? = null
    private var searchMenuItem: MenuItem? = null
    private var sortOrderMenu: Pair<MenuItem, List<MenuItem>>? = null
    private var syncRepositoriesMenuItem: MenuItem? = null
    private var layout: Layout? = null
    private var sectionsList: RecyclerView? = null
    private var sectionsAdapter: SectionsAdapter? = null
    private var viewPager: ViewPager2? = null
    private var onBackPressedCallback: OnBackPressedCallback? = null

    private var showSections = false
        set(value) {
            if (field != value) {
                field = value
                viewModel.showSections.value = value
                val layout = layout
                layout?.tabs?.let {
                    (0 until it.childCount)
                        .forEach { index -> it.getChildAt(index)!!.isEnabled = !value }
                }
                layout?.sectionIcon?.scaleY = if (value) -1f else 1f
                if (((sectionsList?.parent as? View)?.height ?: 0) > 0) {
                    animateSectionsList()
                }
            }
        }

    private var searchQuery = ""

    private val syncConnection = Connection(
        serviceClass = SyncService::class.java,
        onBind = { _, _ ->
            viewPager?.let {
                val source = AppListFragment.Source.entries[it.currentItem]
                updateUpdateNotificationBlocker(source)
            }
        }
    )

    private var sectionsAnimator: ValueAnimator? = null

    private var needSelectUpdates = false

    private val productFragments: Sequence<AppListFragment>
        get() = if (host == null) {
            emptySequence()
        } else {
            childFragmentManager.fragments.asSequence().mapNotNull { it as? AppListFragment }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _tabsBinding = TabsToolbarBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        syncConnection.bind(requireContext())

        sectionsAdapter = SectionsAdapter {
            if (showSections) {
                viewModel.setSection(it)
                sectionsList?.scrollToPosition(0)
                showSections = false
            }
        }

        screenActivity.onToolbarCreated(toolbar)
        toolbar.title = getString(R.string.application_name)
        // Move focus from SearchView to Toolbar
        toolbar.isFocusable = true

        val searchView = FocusSearchView(toolbar.context).apply {
            maxWidth = Int.MAX_VALUE
            queryHint = getString(stringRes.search)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    clearFocus()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (isResumed) {
                        searchQuery = newText.orEmpty()
                        productFragments.forEach { it.setSearchQuery(newText.orEmpty()) }
                    }
                    return true
                }
            })
        }

        toolbar.menu.apply {
            if (!Huawei.isHuaweiEmui) {
                sdkAbove(Build.VERSION_CODES.P) {
                    setGroupDividerEnabled(true)
                }
            }

            searchMenuItem = add(0, R.id.toolbar_search, 0, stringRes.search)
                .setIcon(toolbar.context.getMutatedIcon(CommonR.drawable.ic_search))
                .setActionView(searchView)
                .setShowAsActionFlags(
                    MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW
                )
                .setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                        viewModel.isSearchActionItemExpanded.value = true
                        return true
                    }

                    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                        viewModel.isSearchActionItemExpanded.value = false
                        return true
                    }
                })

            syncRepositoriesMenuItem = add(0, 0, 0, stringRes.sync_repositories)
                .setIcon(toolbar.context.getMutatedIcon(CommonR.drawable.ic_sync))
                .setOnMenuItemClickListener {
//                    SyncWorker.startSyncWork(requireContext())
                    syncConnection.binder?.sync(SyncService.SyncRequest.MANUAL)
                    true
                }

            sortOrderMenu = addSubMenu(0, 0, 0, stringRes.sorting_order)
                .setIcon(toolbar.context.getMutatedIcon(CommonR.drawable.ic_sort))
                .let { menu ->
                    menu.item.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    val menuItems = SortOrder.entries.map { sortOrder ->
                        menu.add(context.sortOrderName(sortOrder))
                            .setOnMenuItemClickListener {
                                viewModel.setSortOrder(sortOrder)
                                true
                            }
                    }
                    menu.setGroupCheckable(0, true, true)
                    Pair(menu.item, menuItems)
                }

            favouritesItem = add(1, 0, 0, stringRes.favourites)
                .setIcon(
                    toolbar.context.getMutatedIcon(CommonR.drawable.ic_favourite_checked)
                )
                .setOnMenuItemClickListener {
                    view.post { screenActivity.navigateFavourites() }
                    true
                }

            add(1, 0, 0, stringRes.repositories)
                .setOnMenuItemClickListener {
                    view.post { screenActivity.navigateRepositories() }
                    true
                }

            add(1, 0, 0, stringRes.settings)
                .setOnMenuItemClickListener {
                    view.post { screenActivity.navigatePreferences() }
                    true
                }
        }

        searchQuery = savedInstanceState?.getString(STATE_SEARCH_QUERY).orEmpty()
        productFragments.forEach { it.setSearchQuery(searchQuery) }

        val toolbarExtra = fragmentBinding.toolbarExtra
        toolbarExtra.addView(tabsBinding.root)
        val layout = Layout(tabsBinding)
        this.layout = layout

        showSections = (savedInstanceState?.getByte(STATE_SHOW_SECTIONS)?.toInt() ?: 0) != 0

        val content = fragmentBinding.fragmentContent

        viewPager = ViewPager2(content.context).apply {
            id = R.id.fragment_pager
            adapter = object : FragmentStateAdapter(this@TabsFragment) {
                override fun getItemCount(): Int = AppListFragment.Source.entries.size
                override fun createFragment(position: Int): Fragment = AppListFragment(
                    AppListFragment.Source.entries[position]
                )
            }
            content.addView(this)
            registerOnPageChangeCallback(pageChangeCallback)
            offscreenPageLimit = 1
        }

        viewPager?.let {
            TabLayoutMediator(layout.tabs, it) { tab, position ->
                tab.text = getString(AppListFragment.Source.entries[position].titleResId)
            }.attach()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.sections.collect(::updateSections)
                }
                launch {
                    viewModel.sortOrder.collect(::updateOrder)
                }
                launch {
                    viewModel.currentSection.collect(::updateSection)
                }
                launch {
                    viewModel.allowHomeScreenSwiping.collect {
                        viewPager?.isUserInputEnabled = it
                    }
                }
                launch {
                    viewModel.backAction.collect {
                        onBackPressedCallback?.isEnabled = it != BackAction.None
                    }
                }
            }
        }

        val backgroundPath = ShapeAppearanceModel.builder()
            .setAllCornerSizes(
                context?.resources?.getDimension(CommonR.dimen.shape_large_corner) ?: 0F
            )
            .build()
        val sectionBackground = MaterialShapeDrawable(backgroundPath)
        val color = SurfaceColors.SURFACE_3.getColor(requireContext())
        sectionBackground.fillColor = ColorStateList.valueOf(color)
        val sectionsList = RecyclerView(toolbar.context).apply {
            id = R.id.sections_list
            layoutManager = LinearLayoutManager(context)
            isMotionEventSplittingEnabled = false
            isVerticalScrollBarEnabled = false
            setHasFixedSize(true)
            adapter = sectionsAdapter
            sectionsAdapter?.let { addDivider(it::configureDivider) }
            background = sectionBackground
            elevation = 4.dp.toFloat()
            content.addView(this)
            val margins = 8.dp
            (layoutParams as ViewGroup.MarginLayoutParams).setMargins(margins, margins, margins, 0)
            visibility = View.GONE
            systemBarsPadding(includeFab = false)
        }
        this.sectionsList = sectionsList

        var lastContentHeight = -1
        content.viewTreeObserver.addOnGlobalLayoutListener {
            if (this.view != null) {
                val initial = lastContentHeight <= 0
                val contentHeight = content.height
                if (lastContentHeight != contentHeight) {
                    lastContentHeight = contentHeight
                    if (initial) {
                        sectionsList.layoutParams.height = if (showSections) contentHeight else 0
                        sectionsList.isVisible = showSections
                        sectionsList.requestLayout()
                    } else {
                        animateSectionsList()
                    }
                }
            }
        }
        onBackPressedCallback = object : OnBackPressedCallback(enabled = false) {
            override fun handleOnBackPressed() {
                performOnBackPressed()
            }
        }
        onBackPressedCallback?.let {
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                it,
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        favouritesItem = null
        searchMenuItem = null
        sortOrderMenu = null
        syncRepositoriesMenuItem = null
        layout = null
        sectionsList = null
        sectionsAdapter = null
        viewPager = null

        syncConnection.unbind(requireContext())
        sectionsAnimator?.cancel()
        sectionsAnimator = null

        _tabsBinding = null
        onBackPressedCallback = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(STATE_SEARCH_FOCUSED, searchMenuItem?.actionView?.hasFocus() == true)
        outState.putString(STATE_SEARCH_QUERY, searchQuery)
        outState.putByte(STATE_SHOW_SECTIONS, if (showSections) 1 else 0)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        (searchMenuItem?.actionView as FocusSearchView).allowFocus = true
        if (needSelectUpdates) {
            needSelectUpdates = false
            selectUpdatesInternal(false)
        }
    }

    private fun performOnBackPressed() {
        when (viewModel.backAction.value) {
            BackAction.ProductAll -> {
                viewModel.setSection(ProductItem.Section.All)
            }

            BackAction.CollapseSearchView -> {
                searchMenuItem?.collapseActionView()
            }

            BackAction.HideSections -> {
                showSections = false
            }

            BackAction.None -> {
                // should never be called
            }
        }
    }

    internal fun selectUpdates() = selectUpdatesInternal(true)

    private fun updateUpdateNotificationBlocker(activeSource: AppListFragment.Source) {
        val blockerFragment = if (activeSource == AppListFragment.Source.UPDATES) {
            productFragments.find { it.source == activeSource }
        } else {
            null
        }
        syncConnection.binder?.setUpdateNotificationBlocker(blockerFragment)
    }

    private fun selectUpdatesInternal(allowSmooth: Boolean) {
        if (view != null) {
            val viewPager = viewPager
            viewPager?.setCurrentItem(
                AppListFragment.Source.UPDATES.ordinal,
                allowSmooth && viewPager.isLaidOut
            )
        } else {
            needSelectUpdates = true
        }
    }

    private fun updateOrder(sortOrder: SortOrder) {
        sortOrderMenu!!.second[sortOrder.ordinal].isChecked = true
    }

    private fun updateSections(
        sectionsList: List<ProductItem.Section>
    ) {
        sectionsAdapter?.sections = sectionsList
        layout?.run {
            sectionIcon.isVisible = sectionsList.any { it !is ProductItem.Section.All }
            sectionLayout.setOnClickListener { showSections = isVisible && !showSections }
        }
    }

    private fun updateSection(section: ProductItem.Section) {
        layout?.sectionName?.text = when (section) {
            is ProductItem.Section.All -> getString(stringRes.all_applications)
            is ProductItem.Section.Category -> section.name
            is ProductItem.Section.Repository -> section.name
        }
        productFragments.filter { it.source.sections }.forEach { it.setSection(section) }
    }

    private fun animateSectionsList() {
        val sectionsList = sectionsList!!
        val value = if (sectionsList.visibility != View.VISIBLE) {
            0f
        } else {
            sectionsList.height.toFloat() / (sectionsList.parent as View).height
        }
        val target = if (showSections) 0.98f else 0f
        sectionsAnimator?.cancel()
        sectionsAnimator = null

        if (value != target) {
            sectionsAnimator = ValueAnimator.ofFloat(value, target).apply {
                duration = (250 * abs(target - value)).toLong()
                interpolator = DecelerateInterpolator(2f)
                addUpdateListener {
                    val newValue = animatedValue as Float
                    sectionsList.apply {
                        val height = ((parent as View).height * newValue).toInt()
                        val visible = height > 0
                        if ((visibility == View.VISIBLE) != visible) isVisible = visible
                        if (layoutParams.height != height) {
                            layoutParams.height = height
                            requestLayout()
                        }
                    }
                    if (target <= 0f && newValue <= 0f) {
                        sectionsAnimator = null
                    }
                }
                start()
            }
        }
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            val layout = layout!!
            val fromSections = AppListFragment.Source.entries[position].sections
            val toSections = if (positionOffset <= 0f) {
                fromSections
            } else {
                AppListFragment.Source.entries[position + 1].sections
            }
            val offset = if (fromSections != toSections) {
                if (fromSections) 1f - positionOffset else positionOffset
            } else {
                if (fromSections) 1f else 0f
            }
            assert(layout.sectionLayout.childCount == 1)
            val child = layout.sectionLayout.getChildAt(0)
            val height = child.layoutParams.height
            assert(height > 0)
            val currentHeight = (offset * height).roundToInt()
            if (layout.sectionLayout.layoutParams.height != currentHeight) {
                layout.sectionLayout.layoutParams.height = currentHeight
                layout.sectionLayout.requestLayout()
            }
        }

        override fun onPageSelected(position: Int) {
            val source = AppListFragment.Source.entries[position]
            updateUpdateNotificationBlocker(source)
            sortOrderMenu!!.first.apply {
                isVisible = source.order
                setShowAsActionFlags(
                    if (!source.order ||
                        resources.configuration.screenWidthDp >= 300
                    ) {
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                    } else {
                        0
                    }
                )
            }
            syncRepositoriesMenuItem!!.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
            if (showSections && !source.sections) {
                showSections = false
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            val source = AppListFragment.Source.entries[viewPager!!.currentItem]
            layout!!.sectionChange.isEnabled =
                state != ViewPager2.SCROLL_STATE_DRAGGING && source.sections
            if (state == ViewPager2.SCROLL_STATE_IDLE) {
                // onPageSelected can be called earlier than fragments created
                updateUpdateNotificationBlocker(source)
            }
        }
    }

    private class SectionsAdapter(
        private val onClick: (ProductItem.Section) -> Unit
    ) : StableRecyclerAdapter<SectionsAdapter.ViewType, RecyclerView.ViewHolder>() {
        enum class ViewType { SECTION }

        private class SectionViewHolder(context: Context) :
            RecyclerView.ViewHolder(FrameLayout(context)) {
            val title: TextView = TextView(context)

            init {
                with(title) {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(16.dp, 0, 16.dp, 0)
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
                with(itemView as FrameLayout) {
                    layoutParams = RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        48.dp
                    )
                    background = context.selectableBackground
                    addView(title)
                }
            }
        }

        var sections: List<ProductItem.Section> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        fun configureDivider(
            context: Context,
            position: Int,
            configuration: DividerConfiguration
        ) {
            val currentSection = sections[position]
            val nextSection = sections.getOrNull(position + 1)
            when {
                nextSection != null && currentSection.javaClass != nextSection.javaClass -> {
                    val padding = context.resources.sizeScaled(16)
                    configuration.set(
                        needDivider = true,
                        toTop = false,
                        paddingStart = padding,
                        paddingEnd = padding
                    )
                }

                else -> {
                    configuration.set(
                        needDivider = false,
                        toTop = false,
                        paddingStart = 0,
                        paddingEnd = 0
                    )
                }
            }
        }

        override val viewTypeClass: Class<ViewType>
            get() = ViewType::class.java

        override fun getItemCount(): Int = sections.size
        override fun getItemDescriptor(position: Int): String = sections[position].toString()
        override fun getItemEnumViewType(position: Int): ViewType = ViewType.SECTION

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: ViewType
        ): RecyclerView.ViewHolder {
            return SectionViewHolder(parent.context).apply {
                itemView.setOnClickListener { onClick(sections[absoluteAdapterPosition]) }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder as SectionViewHolder
            val section = sections[position]
            val previousSection = sections.getOrNull(position - 1)
            val nextSection = sections.getOrNull(position + 1)
            val margin = holder.itemView.resources.sizeScaled(8)
            val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
            layoutParams.topMargin = if (previousSection == null ||
                section.javaClass != previousSection.javaClass
            ) {
                margin
            } else {
                0
            }
            layoutParams.bottomMargin = if (nextSection == null ||
                section.javaClass != nextSection.javaClass
            ) {
                margin
            } else {
                0
            }
            holder.title.text = when (section) {
                is ProductItem.Section.All -> holder.itemView.resources.getString(
                    stringRes.all_applications
                )

                is ProductItem.Section.Category -> section.name
                is ProductItem.Section.Repository -> section.name
            }
        }
    }
}
