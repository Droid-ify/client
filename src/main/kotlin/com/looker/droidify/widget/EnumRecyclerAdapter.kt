package com.looker.droidify.widget

import android.util.SparseArray
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

abstract class EnumRecyclerAdapter<VT: Enum<VT>, VH: RecyclerView.ViewHolder>: RecyclerView.Adapter<VH>() {
  abstract val viewTypeClass: Class<VT>

  private val names = SparseArray<String>()

  private fun getViewType(viewType: Int): VT {
    return java.lang.Enum.valueOf(viewTypeClass, names.get(viewType))
  }

  final override fun getItemViewType(position: Int): Int {
    val enum = getItemEnumViewType(position)
    names.put(enum.ordinal, enum.name)
    return enum.ordinal
  }

  final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    return onCreateViewHolder(parent, getViewType(viewType))
  }

  abstract fun getItemEnumViewType(position: Int): VT
  abstract fun onCreateViewHolder(parent: ViewGroup, viewType: VT): VH
}
