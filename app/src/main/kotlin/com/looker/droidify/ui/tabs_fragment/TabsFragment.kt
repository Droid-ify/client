package com.looker.droidify.ui.tabs_fragment

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
import android.widget.TextView
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
import com.looker.core.common.extension.getDrawableFromAttr
import com.looker.core.common.extension.systemBarsPadding
import com.looker.core.common.sdkAbove
import com.looker.core.datastore.extension.sortOrderName
import com.looker.core.datastore.model.SortOrder
import com.looker.core.model.ProductItem
import com.looker.droidify.R
import com.looker.droidify.database.Database
import com.looker.droidify.databinding.TabsToolbarBinding
import com.looker.droidify.screen.ScreenFragment
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.ui.app_list.AppListFragment
import com.looker.droidify.utility.Utils
import com.looker.droidify.utility.extension.resources.sizeScaled
import com.looker.droidify.utility.extension.screenActivity
import com.looker.droidify.widget.DividerItemDecoration
import com.looker.droidify.widget.FocusSearchView
import com.looker.droidify.widget.StableRecyclerAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import com.looker.core.common.R as CommonR
import com.looker.core.common.R.string as stringRes

@AndroidEntryPoint
class TabsFragment : ScreenFragment() {

	private var _tabsBinding: TabsToolbarBinding? = null
	private val tabsBinding get() = _tabsBinding!!

	private val viewModel: TabsViewModel by viewModels()

	companion object {
		private const val STATE_SEARCH_FOCUSED = "searchFocused"
		private const val STATE_SEARCH_QUERY = "searchQuery"
		private const val STATE_SHOW_SECTIONS = "showSections"
		private const val STATE_SECTIONS = "sections"
		private const val STATE_SECTION = "section"
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
	private var viewPager: ViewPager2? = null

	private var showSections = false
		set(value) {
			if (field != value) {
				field = value
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
	private var sections = listOf<ProductItem.Section>(ProductItem.Section.All)
	private var section: ProductItem.Section = ProductItem.Section.All

	private val syncConnection = Connection(SyncService::class.java)

	private var sectionsAnimator: ValueAnimator? = null

	private var needSelectUpdates = false

	private val productFragments: Sequence<AppListFragment>
		get() = if (host == null) emptySequence() else
			childFragmentManager.fragments.asSequence().mapNotNull { it as? AppListFragment }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		_tabsBinding = TabsToolbarBinding.inflate(layoutInflater)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		syncConnection.bind(requireContext())

		screenActivity.onToolbarCreated(toolbar)
		collapsingToolbar.title = getString(R.string.application_name)
		// Move focus from SearchView to Toolbar
		toolbar.isFocusableInTouchMode = true

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
			setOnSearchClickListener { fragmentBinding.appbarLayout.setExpanded(false, true) }
		}

		toolbar.menu.apply {
			if (!Huawei.isHuaweiEmui) {
				sdkAbove(Build.VERSION_CODES.P) {
					setGroupDividerEnabled(true)
				}
			}

			searchMenuItem = add(0, R.id.toolbar_search, 0, stringRes.search)
				.setIcon(Utils.getToolbarIcon(toolbar.context, CommonR.drawable.ic_search))
				.setActionView(searchView)
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)

			sortOrderMenu = addSubMenu(0, 0, 0, stringRes.sorting_order)
				.setIcon(Utils.getToolbarIcon(toolbar.context, CommonR.drawable.ic_sort))
				.let { menu ->
					menu.item.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
					val menuItems = SortOrder.values().map { sortOrder ->
						menu.add(context.sortOrderName(sortOrder))
							.setOnMenuItemClickListener {
								viewModel.setSortOrder(sortOrder)
								true
							}
					}
					menu.setGroupCheckable(0, true, true)
					Pair(menu.item, menuItems)
				}

			syncRepositoriesMenuItem = add(0, 0, 0, stringRes.sync_repositories)
				.setIcon(Utils.getToolbarIcon(toolbar.context, CommonR.drawable.ic_sync))
				.setOnMenuItemClickListener {
					syncConnection.binder?.sync(SyncService.SyncRequest.MANUAL)
					true
				}

			favouritesItem = add(1, 0, 0, stringRes.favourites)
				.setIcon(
					Utils.getToolbarIcon(
						toolbar.context,
						CommonR.drawable.ic_favourite_checked
					)
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
		sections = savedInstanceState?.getParcelableArrayList<ProductItem.Section>(STATE_SECTIONS)
			.orEmpty()
		section = savedInstanceState?.getParcelable(STATE_SECTION) ?: ProductItem.Section.All
		layout.sectionChange.setOnClickListener {
			showSections = sections
				.any { it !is ProductItem.Section.All } && !showSections
		}

		viewLifecycleOwner.lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.CREATED) {
				viewModel.sortOrderFlow.collect {
					updateOrder(it)
				}
			}
		}

		val content = fragmentBinding.fragmentContent

		viewPager = ViewPager2(content.context).apply {
			id = R.id.fragment_pager
			adapter = object : FragmentStateAdapter(this@TabsFragment) {
				override fun getItemCount(): Int = AppListFragment.Source.values().size
				override fun createFragment(position: Int): Fragment = AppListFragment(
					AppListFragment
						.Source.values()[position]
				)
			}
			content.addView(this)
			registerOnPageChangeCallback(pageChangeCallback)
			offscreenPageLimit = 1
		}

		viewPager?.let {
			TabLayoutMediator(layout.tabs, it) { tab, position ->
				tab.text = getString(AppListFragment.Source.values()[position].titleResId)
			}.attach()
		}

		viewLifecycleOwner.lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.RESUMED) {
				launch {
					flowOf(Unit)
						.onCompletion { if (it == null) emitAll(Database.flowCollection(Database.Subject.Products)) }
						.map { Database.CategoryAdapter.getAll(null) }
						.collectLatest {
							setSectionsAndUpdate(
								it.asSequence().sorted()
									.map(ProductItem.Section::Category).toList(), null
							)
						}
				}
				launch {
					flowOf(Unit)
						.onCompletion { if (it == null) emitAll(Database.flowCollection(Database.Subject.Repositories)) }
						.map { Database.RepositoryAdapter.getAll(null) }
						.collectLatest { repos ->
							setSectionsAndUpdate(null, repos.asSequence().filter { it.enabled }
								.map { ProductItem.Section.Repository(it.id, it.name) }.toList())
						}
				}
			}
		}
		updateSection()

		val backgroundPath = ShapeAppearanceModel.builder()
			.setAllCornerSizes(
				context?.resources?.getDimension(CommonR.dimen.shape_medium_corner) ?: 0F
			)
			.build()
		val background = MaterialShapeDrawable(backgroundPath)
		val color = SurfaceColors.SURFACE_3.getColor(requireContext())
		background.fillColor = ColorStateList.valueOf(color)
		val sectionsList = RecyclerView(toolbar.context).apply {
			id = R.id.sections_list
			layoutManager = LinearLayoutManager(context)
			isMotionEventSplittingEnabled = false
			isVerticalScrollBarEnabled = false
			setHasFixedSize(true)
			val adapter = SectionsAdapter({ sections }) {
				if (showSections) {
					scrollToPosition(0)
					showSections = false
					section = it
					updateSection()
				}
			}
			this.adapter = adapter
			addItemDecoration(DividerItemDecoration(context, adapter::configureDivider))
			this.background = background
			elevation = resources.sizeScaled(4).toFloat()
			content.addView(this)
			val margins = resources.sizeScaled(8)
			(layoutParams as ViewGroup.MarginLayoutParams).setMargins(margins, margins, margins, 0)
			visibility = View.GONE
			systemBarsPadding()
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
	}

	override fun onDestroyView() {
		super.onDestroyView()

		favouritesItem = null
		searchMenuItem = null
		sortOrderMenu = null
		syncRepositoriesMenuItem = null
		layout = null
		sectionsList = null
		viewPager = null

		syncConnection.unbind(requireContext())
		sectionsAnimator?.cancel()
		sectionsAnimator = null

		_tabsBinding = null
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)

		outState.putBoolean(STATE_SEARCH_FOCUSED, searchMenuItem?.actionView?.hasFocus() == true)
		outState.putString(STATE_SEARCH_QUERY, searchQuery)
		outState.putByte(STATE_SHOW_SECTIONS, if (showSections) 1 else 0)
		outState.putParcelableArrayList(STATE_SECTIONS, ArrayList(sections))
		outState.putParcelable(STATE_SECTION, section)
	}

	override fun onViewStateRestored(savedInstanceState: Bundle?) {
		super.onViewStateRestored(savedInstanceState)

		(searchMenuItem?.actionView as FocusSearchView).allowFocus = true
		if (needSelectUpdates) {
			needSelectUpdates = false
			selectUpdatesInternal(false)
		}
	}

	override fun onBackPressed(): Boolean {
		return when {
			searchMenuItem?.isActionViewExpanded == true -> {
				searchMenuItem?.collapseActionView()
				true
			}
			showSections -> {
				showSections = false
				true
			}
			else -> {
				super.onBackPressed()
			}
		}
	}

	internal fun selectUpdates() = selectUpdatesInternal(true)

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
		productFragments.forEach { it.setOrder() }
	}

	private inline fun <reified T : ProductItem.Section> collectOldSections(list: List<T>?): List<T>? {
		val oldList = sections.mapNotNull { it as? T }
		return if (list == null || oldList == list) oldList else null
	}

	private fun setSectionsAndUpdate(
		categories: List<ProductItem.Section.Category>?,
		repositories: List<ProductItem.Section.Repository>?,
	) {
		val oldCategories = collectOldSections(categories)
		val oldRepositories = collectOldSections(repositories)
		if (oldCategories == null || oldRepositories == null) {
			sections = listOf(ProductItem.Section.All) +
					(categories ?: oldCategories).orEmpty() +
					(repositories ?: oldRepositories).orEmpty()
			updateSection()
		}
	}

	private fun updateSection() {
		if (section !in sections) {
			section = ProductItem.Section.All
		}
		layout?.sectionName?.text = when (val section = section) {
			is ProductItem.Section.All -> getString(stringRes.all_applications)
			is ProductItem.Section.Category -> section.name
			is ProductItem.Section.Repository -> section.name
		}
		layout?.sectionIcon?.isVisible = sections.any { it !is ProductItem.Section.All }
		productFragments.forEach { it.setSection(section) }
		sectionsList?.adapter?.notifyDataSetChanged()
	}

	private fun animateSectionsList() {
		val sectionsList = sectionsList!!
		val value = if (sectionsList.visibility != View.VISIBLE) 0f else
			sectionsList.height.toFloat() / (sectionsList.parent as View).height
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
			positionOffsetPixels: Int,
		) {
			val layout = layout!!
			val fromSections = AppListFragment.Source.values()[position].sections
			val toSections = if (positionOffset <= 0f) fromSections else
				AppListFragment.Source.values()[position + 1].sections
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
			val source = AppListFragment.Source.values()[position]
			sortOrderMenu!!.first.apply {
				isVisible = source.order
				setShowAsActionFlags(
					if (!source.order ||
						resources.configuration.screenWidthDp >= 400
					) MenuItem.SHOW_AS_ACTION_ALWAYS else 0
				)
			}
			syncRepositoriesMenuItem!!.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
			if (showSections && !source.sections) {
				showSections = false
			}
		}

		override fun onPageScrollStateChanged(state: Int) {
			val source = AppListFragment.Source.values()[viewPager!!.currentItem]
			layout!!.sectionChange.isEnabled =
				state != ViewPager2.SCROLL_STATE_DRAGGING && source.sections
		}
	}

	private class SectionsAdapter(
		private val sections: () -> List<ProductItem.Section>,
		private val onClick: (ProductItem.Section) -> Unit,
	) : StableRecyclerAdapter<SectionsAdapter.ViewType,
			RecyclerView.ViewHolder>() {
		enum class ViewType { SECTION }

		private class SectionViewHolder(context: Context) :
			RecyclerView.ViewHolder(TextView(context)) {
			val title: TextView
				get() = itemView as TextView

			init {
				itemView as TextView
				itemView.gravity = Gravity.CENTER_VERTICAL
				itemView.resources.sizeScaled(16).let { itemView.setPadding(it, 0, it, 0) }
				itemView.background =
					context.getDrawableFromAttr(android.R.attr.selectableItemBackground)
				itemView.layoutParams = RecyclerView.LayoutParams(
					RecyclerView.LayoutParams.MATCH_PARENT,
					itemView.resources.sizeScaled(48)
				)
			}
		}

		fun configureDivider(
			context: Context,
			position: Int,
			configuration: DividerItemDecoration.Configuration,
		) {
			val currentSection = sections()[position]
			val nextSection = sections().getOrNull(position + 1)
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

		override fun getItemCount(): Int = sections().size
		override fun getItemDescriptor(position: Int): String = sections()[position].toString()
		override fun getItemEnumViewType(position: Int): ViewType = ViewType.SECTION

		override fun onCreateViewHolder(
			parent: ViewGroup,
			viewType: ViewType,
		): RecyclerView.ViewHolder {
			return SectionViewHolder(parent.context).apply {
				itemView.setOnClickListener { onClick(sections()[absoluteAdapterPosition]) }
			}
		}

		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			holder as SectionViewHolder
			val section = sections()[position]
			val previousSection = sections().getOrNull(position - 1)
			val nextSection = sections().getOrNull(position + 1)
			val margin = holder.itemView.resources.sizeScaled(8)
			val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
			layoutParams.topMargin = if (previousSection == null ||
				section.javaClass != previousSection.javaClass
			) margin else 0
			layoutParams.bottomMargin = if (nextSection == null ||
				section.javaClass != nextSection.javaClass
			) margin else 0
			holder.title.text = when (section) {
				is ProductItem.Section.All -> holder.itemView.resources.getString(stringRes.all_applications)
				is ProductItem.Section.Category -> section.name
				is ProductItem.Section.Repository -> section.name
			}
		}
	}
}
