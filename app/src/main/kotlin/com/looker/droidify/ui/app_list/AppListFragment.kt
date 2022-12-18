package com.looker.droidify.ui.app_list

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.looker.core.common.view.systemBarsMargin
import com.looker.core.model.ProductItem
import com.looker.droidify.database.CursorOwner
import com.looker.droidify.database.Database
import com.looker.droidify.databinding.RecyclerViewWithFabBinding
import com.looker.droidify.utility.extension.screenActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import com.looker.core.common.R.string as stringRes

@AndroidEntryPoint
class AppListFragment() : Fragment(), CursorOwner.Callback {

	private val viewModel: AppListViewModel by viewModels()

	private var _binding: RecyclerViewWithFabBinding? = null
	private val binding get() = _binding!!

	companion object {
		private const val EXTRA_SOURCE = "source"
	}

	enum class Source(val titleResId: Int, val sections: Boolean, val order: Boolean) {
		AVAILABLE(stringRes.available, true, true),
		INSTALLED(stringRes.installed, false, true),
		UPDATES(stringRes.updates, false, false)
	}

	constructor(source: Source) : this() {
		arguments = Bundle().apply {
			putString(EXTRA_SOURCE, source.name)
		}
	}

	val source: Source
		get() = requireArguments().getString(EXTRA_SOURCE)!!.let(Source::valueOf)


	private lateinit var recyclerViewAdapter: AppListAdapter

	private var shortAnimationDuration: Int = 0

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		_binding = RecyclerViewWithFabBinding.inflate(inflater, container, false)

		shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

		val recyclerView = binding.recyclerView.apply {
			id = android.R.id.list
			layoutManager = LinearLayoutManager(context)
			isMotionEventSplittingEnabled = false
			isVerticalScrollBarEnabled = false
			setHasFixedSize(true)
			recycledViewPool.setMaxRecycledViews(AppListAdapter.ViewType.PRODUCT.ordinal, 30)
			recyclerViewAdapter = AppListAdapter { screenActivity.navigateProduct(it.packageName) }
			this.adapter = recyclerViewAdapter
		}
		val fab = binding.scrollUp
		fab.setOnClickListener { recyclerView.smoothScrollToPosition(0) }
		fab.apply {
			this.alpha = 0f
			visibility = View.VISIBLE
			systemBarsMargin()
		}

		val scrollListener = object : RecyclerView.OnScrollListener() {
			override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
				val position = (recyclerView.layoutManager as LinearLayoutManager)
					.findFirstVisibleItemPosition()
				fab.animate()
					.alpha(if (position != 0) 1f else 0f)
					.setDuration(shortAnimationDuration.toLong())
					.setListener(null)
			}
		}
		recyclerView.addOnScrollListener(scrollListener)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		screenActivity.cursorOwner.attach(this, viewModel.request(source))
		viewLifecycleOwner.lifecycleScope.launch {
			flowOf(Unit)
				.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.RESUMED)
				.onCompletion { if (it == null) emitAll(Database.flowCollection(Database.Subject.Repositories)) }
				.map { Database.RepositoryAdapter.getAll(null).associateBy { it.id } }
				.collectLatest { recyclerViewAdapter.repositories = it }
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
		screenActivity.cursorOwner.detach(this)
	}

	override fun onCursorData(request: CursorOwner.Request, cursor: Cursor?) {
		recyclerViewAdapter.apply {
			this.cursor = cursor
			lifecycleScope.launch {
				repeatOnLifecycle(Lifecycle.State.RESUMED) {
					emptyText = when {
						cursor == null -> ""
						viewModel.searchQuery.first()
							.isNotEmpty() -> getString(stringRes.no_matching_applications_found)
						else -> when (source) {
							Source.AVAILABLE -> getString(stringRes.no_applications_available)
							Source.INSTALLED -> getString(stringRes.no_applications_installed)
							Source.UPDATES -> getString(stringRes.all_applications_up_to_date)
						}
					}
				}
			}
		}
	}

	internal fun setSearchQuery(searchQuery: String) {
		viewModel.setSearchQuery(searchQuery) {
			if (view != null) {
				screenActivity.cursorOwner.attach(this, viewModel.request(source))
			}
		}
	}

	internal fun setSection(section: ProductItem.Section) {
		viewModel.setSection(section) {
			if (view != null) {
				screenActivity.cursorOwner.attach(this, viewModel.request(source))
			}
		}
	}

	internal fun setOrder() {
		if (view != null) {
			screenActivity.cursorOwner.attach(this, viewModel.request(source))
		}
	}
}
