package com.looker.droidify.widget

import androidx.recyclerview.widget.RecyclerView

abstract class StableRecyclerAdapter<VT: Enum<VT>, VH: RecyclerView.ViewHolder>: EnumRecyclerAdapter<VT, VH>() {
  private var nextId = 1L
  private val descriptorToId = mutableMapOf<String, Long>()

  init {
    super.setHasStableIds(true)
  }

  final override fun setHasStableIds(hasStableIds: Boolean) {
    throw UnsupportedOperationException()
  }

  override fun getItemId(position: Int): Long {
    val descriptor = getItemDescriptor(position)
    return descriptorToId[descriptor] ?: run {
      val id = nextId++
      descriptorToId[descriptor] = id
      id
    }
  }

  abstract fun getItemDescriptor(position: Int): String
}
