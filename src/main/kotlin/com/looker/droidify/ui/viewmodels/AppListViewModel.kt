package com.looker.droidify.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.droidify.database.CursorOwner
import com.looker.droidify.entity.ProductItem
import com.looker.droidify.ui.fragments.AppListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppListViewModel : ViewModel() {

    private val _order = MutableStateFlow(ProductItem.Order.LAST_UPDATE)
    private val _sections = MutableStateFlow<ProductItem.Section>(ProductItem.Section.All)
    private val _searchQuery = MutableStateFlow("")

    val order: StateFlow<ProductItem.Order> = _order.stateIn(
        initialValue = ProductItem.Order.LAST_UPDATE,
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000)
    )

    val sections: StateFlow<ProductItem.Section> = _sections.stateIn(
        initialValue = ProductItem.Section.All,
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000)
    )
    val searchQuery: StateFlow<String> = _searchQuery.stateIn(
        initialValue = "",
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000)
    )

    fun request(source: AppListFragment.Source): CursorOwner.Request {
        var mSearchQuery = ""
        var mSections: ProductItem.Section = ProductItem.Section.All
        var mOrder: ProductItem.Order = ProductItem.Order.NAME
        viewModelScope.launch {
            launch { searchQuery.collect { if (source.sections) mSearchQuery = it } }
            launch { sections.collect { if (source.sections) mSections = it } }
            launch { order.collect { if (source.order) mOrder = it } }
        }
        return when (source) {
            AppListFragment.Source.AVAILABLE -> CursorOwner.Request.ProductsAvailable(
                mSearchQuery,
                mSections,
                mOrder
            )
            AppListFragment.Source.INSTALLED -> CursorOwner.Request.ProductsInstalled(
                mSearchQuery,
                mSections,
                mOrder
            )
            AppListFragment.Source.UPDATES -> CursorOwner.Request.ProductsUpdates(
                mSearchQuery,
                mSections,
                mOrder
            )
        }
    }

    fun setSection(newSection: ProductItem.Section, perform: () -> Unit) {
        viewModelScope.launch {
            if (newSection != sections.value) {
                _sections.emit(newSection)
                launch(Dispatchers.Main) { perform() }
            }
        }
    }

    fun setOrder(newOrder: ProductItem.Order, perform: () -> Unit) {
        viewModelScope.launch {
            if (newOrder != order.value) {
                _order.emit(newOrder)
                launch(Dispatchers.Main) { perform() }
            }
        }
    }

    fun setSearchQuery(newSearchQuery: String, perform: () -> Unit) {
        viewModelScope.launch {
            if (newSearchQuery != searchQuery.value) {
                _searchQuery.emit(newSearchQuery)
                launch(Dispatchers.Main) { perform() }
            }
        }
    }
}