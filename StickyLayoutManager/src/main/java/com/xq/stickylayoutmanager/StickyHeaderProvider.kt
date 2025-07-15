package com.xq.stickylayoutmanager

import androidx.recyclerview.widget.RecyclerView

fun interface StickyHeaderProvider {
    fun isStickyHeader(adapter: RecyclerView.Adapter<*>, position: Int): Boolean
}
