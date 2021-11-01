package com.looker.droidify.screen

import android.database.Cursor
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.looker.droidify.R
import com.looker.droidify.database.CursorOwner
import com.looker.droidify.database.Database
import com.looker.droidify.entity.ProductItem
import com.looker.droidify.ui.ProductsViewModel
import com.looker.droidify.utility.RxUtils
import com.looker.droidify.widget.RecyclerFastScroller
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.collect

class ProductsFragment() : BaseFragment(), CursorOwner.Callback {

    private val viewModel: ProductsViewModel by viewModels()

    companion object {
        private const val EXTRA_SOURCE = "source"
        private const val STATE_LAYOUT_MANAGER = "layoutManager"
    }

    enum class Source(val titleResId: Int, val sections: Boolean, val order: Boolean) {
        AVAILABLE(R.string.available, true, true),
        INSTALLED(R.string.installed, false, false),
        UPDATES(R.string.updates, false, false)
    }

    constructor(source: Source) : this() {
        arguments = Bundle().apply {
            putString(EXTRA_SOURCE, source.name)
        }
    }

    val source: Source
        get() = requireArguments().getString(EXTRA_SOURCE)!!.let(Source::valueOf)

    private val searchQuery: String
        get() {
            var _searchQuery = ""
            lifecycleScope.launchWhenCreated { viewModel.searchQuery.collect { _searchQuery = it } }
            return _searchQuery
        }
    private val section: ProductItem.Section
        get() {
            var _section: ProductItem.Section = ProductItem.Section.All
            lifecycleScope.launchWhenCreated { viewModel.sections.collect { _section = it } }
            return _section
        }
    private val order: ProductItem.Order
        get() {
            var _order: ProductItem.Order = ProductItem.Order.LAST_UPDATE
            lifecycleScope.launchWhenCreated { viewModel.order.collect { _order = it } }
            return _order
        }

    private var layoutManagerState: Parcelable? = null

    private var recyclerView: RecyclerView? = null

    private var repositoriesDisposable: Disposable? = null

    private val request: CursorOwner.Request
        get() {
            val searchQuery = searchQuery
            val section = if (source.sections) section else ProductItem.Section.All
            val order = if (source.order) order else ProductItem.Order.NAME
            return when (source) {
                Source.AVAILABLE -> CursorOwner.Request.ProductsAvailable(
                    searchQuery,
                    section,
                    order
                )
                Source.INSTALLED -> CursorOwner.Request.ProductsInstalled(
                    searchQuery,
                    section,
                    order
                )
                Source.UPDATES -> CursorOwner.Request.ProductsUpdates(searchQuery, section, order)
            }
        }

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
            recycledViewPool.setMaxRecycledViews(ProductsAdapter.ViewType.PRODUCT.ordinal, 30)
            val adapter = ProductsAdapter { screenActivity.navigateProduct(it.packageName) }
            this.adapter = adapter
            RecyclerFastScroller(this)
            recyclerView = this
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        layoutManagerState = savedInstanceState?.getParcelable(STATE_LAYOUT_MANAGER)

        screenActivity.cursorOwner.attach(this, request)
        repositoriesDisposable = Observable.just(Unit)
            .concatWith(Database.observable(Database.Subject.Repositories))
            .observeOn(Schedulers.io())
            .flatMapSingle { RxUtils.querySingle { Database.RepositoryAdapter.getAll(it) } }
            .map { it.asSequence().map { Pair(it.id, it) }.toMap() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { (recyclerView?.adapter as? ProductsAdapter)?.repositories = it }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        recyclerView = null

        screenActivity.cursorOwner.detach(this)
        repositoriesDisposable?.dispose()
        repositoriesDisposable = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        (layoutManagerState ?: recyclerView?.layoutManager?.onSaveInstanceState())
            ?.let { outState.putParcelable(STATE_LAYOUT_MANAGER, it) }
    }

    override fun onCursorData(request: CursorOwner.Request, cursor: Cursor?) {
        (recyclerView?.adapter as? ProductsAdapter)?.apply {
            this.cursor = cursor
            emptyText = when {
                cursor == null -> ""
                searchQuery.isNotEmpty() -> getString(R.string.no_matching_applications_found)
                else -> when (source) {
                    Source.AVAILABLE -> getString(R.string.no_applications_available)
                    Source.INSTALLED -> getString(R.string.no_applications_installed)
                    Source.UPDATES -> getString(R.string.all_applications_up_to_date)
                }
            }
        }
        layoutManagerState?.let {
            layoutManagerState = null
            recyclerView?.layoutManager?.onRestoreInstanceState(it)
        }
    }

    internal fun setSearchQuery(searchQuery: String) {
        viewModel.setSearchQuery(searchQuery) {
            if (view != null) {
                screenActivity.cursorOwner.attach(this, request)
            }
        }
    }

    internal fun setSection(section: ProductItem.Section) {
        viewModel.setSection(section) {
            if (view != null) {
                screenActivity.cursorOwner.attach(this, request)
            }
        }
    }

    internal fun setOrder(order: ProductItem.Order) {
        viewModel.setOrder(order) {
            if (view != null) {
                screenActivity.cursorOwner.attach(this, request)
            }
        }
    }
}
