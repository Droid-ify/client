package com.looker.droidify.datastore.model

// todo: Add Support for sorting by size
enum class SortOrder {
    UPDATED,
    ADDED,
    NAME,
    SIZE,
}

fun supportedSortOrders(): List<SortOrder> = listOf(SortOrder.UPDATED, SortOrder.ADDED, SortOrder.NAME)
