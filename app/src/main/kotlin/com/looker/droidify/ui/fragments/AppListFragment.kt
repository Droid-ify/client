package com.looker.droidify.ui.fragments

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.looker.droidify.R
import com.looker.droidify.database.CursorOwner
import com.looker.droidify.database.Database
import com.looker.droidify.entity.ProductItem
import com.looker.droidify.screen.BaseFragment
import com.looker.droidify.ui.adapters.AppListAdapter
import com.looker.droidify.ui.viewmodels.AppListViewModel
import com.looker.droidify.utility.RxUtils
import com.looker.droidify.utility.extension.resources.getDrawableCompat
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.zhanghai.android.fastscroll.FastScrollerBuilder

class AppListFragment() : BaseFragment(), CursorOwner.Callback {

	private val viewModel: AppListViewModel by viewModels()

	companion object {
		private const val EXTRA_SOURCE = "source"
	}

	enum class Source(val titleResId: Int, val sections: Boolean, val order: Boolean) {
		AVAILABLE(R.string.available, true, true),
		INSTALLED(R.string.installed, false, true),
		UPDATES(R.string.updates, false, false)
	}

	constructor(source: Source) : this() {
		arguments = Bundle().apply {
			putString(EXTRA_SOURCE, source.name)
		}
	}

	val source: Source
		get() = requireArguments().getString(EXTRA_SOURCE)!!.let(Source::valueOf)

	private var recyclerView: RecyclerView? = null

	private var repositoriesDisposable: Disposable? = null

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		return RecyclerView(requireContext()).apply {
			id = android.R.id.list
			layoutManager = LinearLayoutManager(context)
			isMotionEventSplittingEnabled = false
			isVerticalScrollBarEnabled = false
			setHasFixedSize(true)
			recycledViewPool.setMaxRecycledViews(AppListAdapter.ViewType.PRODUCT.ordinal, 30)
			val adapter = AppListAdapter { screenActivity.navigateProduct(it.packageName) }
			this.adapter = adapter
			FastScrollerBuilder(this)
				.setThumbDrawable(context.getDrawableCompat(R.drawable.scrollbar_thumb))
				.setTrackDrawable(context.getDrawableCompat(R.drawable.scrollbar_track))
				.build()
			recyclerView = this
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		screenActivity.cursorOwner.attach(this, viewModel.request(source))
		repositoriesDisposable = Observable.just(Unit)
			.concatWith(Database.observable(Database.Subject.Repositories))
			.observeOn(Schedulers.io())
			.flatMapSingle { RxUtils.querySingle { Database.RepositoryAdapter.getAll(it) } }
			.map { list -> list.asSequence().map { Pair(it.id, it) }.toMap() }
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe { (recyclerView?.adapter as? AppListAdapter)?.repositories = it }
	}

	override fun onDestroyView() {
		super.onDestroyView()

		recyclerView = null

		screenActivity.cursorOwner.detach(this)
		repositoriesDisposable?.dispose()
		repositoriesDisposable = null
	}

	override fun onCursorData(request: CursorOwner.Request, cursor: Cursor?) {
		(recyclerView?.adapter as? AppListAdapter)?.apply {
			this.cursor = cursor
			lifecycleScope.launch {
				repeatOnLifecycle(Lifecycle.State.RESUMED) {
					emptyText = when {
						cursor == null -> ""
						viewModel.searchQuery.first()
							.isNotEmpty() -> getString(R.string.no_matching_applications_found)
						else -> when (source) {
							Source.AVAILABLE -> getString(R.string.no_applications_available)
							Source.INSTALLED -> getString(R.string.no_applications_installed)
							Source.UPDATES -> getString(R.string.all_applications_up_to_date)
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

	internal fun setOrder(order: ProductItem.Order) {
		viewModel.setOrder(order) {
			if (view != null) {
				screenActivity.cursorOwner.attach(this, viewModel.request(source))
			}
		}
	}
}
