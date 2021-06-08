package com.looker.droidify.widget

import android.database.Cursor
import androidx.recyclerview.widget.RecyclerView

abstract class CursorRecyclerAdapter<VT : Enum<VT>, VH : RecyclerView.ViewHolder> :
    EnumRecyclerAdapter<VT, VH>() {
    init {
        super.setHasStableIds(true)
    }

    private var rowIdIndex = 0

    var cursor: Cursor? = null
        set(value) {
            if (field != value) {
                field?.close()
                field = value
                rowIdIndex = value?.getColumnIndexOrThrow("_id") ?: 0
                notifyDataSetChanged()
            }
        }

    final override fun setHasStableIds(hasStableIds: Boolean) {
        throw UnsupportedOperationException()
    }

    override fun getItemCount(): Int = cursor?.count ?: 0
    override fun getItemId(position: Int): Long = moveTo(position).getLong(rowIdIndex)

    fun moveTo(position: Int): Cursor {
        val cursor = cursor!!
        cursor.moveToPosition(position)
        return cursor
    }
}
