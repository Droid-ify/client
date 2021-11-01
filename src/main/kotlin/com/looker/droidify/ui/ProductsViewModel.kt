package com.looker.droidify.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.droidify.entity.ProductItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductsViewModel : ViewModel() {

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

    fun setSection(newSection: ProductItem.Section, perform: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            if (newSection != sections.value) {
                _sections.emit(newSection)
                perform()
            }
        }
    }

    fun setOrder(newOrder: ProductItem.Order, perform: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            if (newOrder != order.value) {
                _order.emit(newOrder)
                perform()
            }
        }
    }

    fun setSearchQuery(newSearchQuery: String, perform: () -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            if (newSearchQuery != searchQuery.value) {
                _searchQuery.emit(newSearchQuery)
                perform()
            }
        }
    }
}