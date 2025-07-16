package com.xq.stickylayoutmanager

import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.atomic.AtomicInteger

internal fun RecyclerView.Adapter<*>.offsetPositionOnAdapter(
    position: Int,
): Pair<RecyclerView.Adapter<*>, Int> {
    if (this is ConcatAdapter) {
        return this.offsetPositionOnConcatAdapter(position)
    }
    return this to position
}

internal fun ConcatAdapter.offsetPositionOnConcatAdapter(position: Int): Pair<RecyclerView.Adapter<*>, Int> {
    for ((key, value ) in this.adapters.getAllAdapterStartPositionMap(AtomicInteger(0)).entries.reversed()) {
        if (position + 1 > value) {
            return Pair(key, position - value)
        }
    }
    return this to position
}

internal fun List<RecyclerView.Adapter<*>>.getAllAdapterStartPositionMap(
    startPosition: AtomicInteger,
): Map<RecyclerView.Adapter<*>, Int> {
    val adapterStartPositionMap: MutableMap<RecyclerView.Adapter<*>, Int> = LinkedHashMap()
    for (adapter in this) {
        if (adapter is ConcatAdapter) {
            adapterStartPositionMap.putAll(adapter.adapters.getAllAdapterStartPositionMap(startPosition))
        } else {
            adapterStartPositionMap.put(adapter, startPosition.get())
            startPosition.set(startPosition.get() + adapter.itemCount)
        }
    }
    return adapterStartPositionMap
}

internal fun ConcatAdapter.getBeforeCount(dest: RecyclerView.Adapter<*>?): Int {
    val count = AtomicInteger(0)
    if (this.findAndCountBefore(dest, count)) {
        return count.get()
    }
    return 0
}

internal fun ConcatAdapter.findAndCountBefore(dest: RecyclerView.Adapter<*>?, count: AtomicInteger): Boolean {
    for (adapter in this.adapters) {
        if (adapter === dest) {
            return true
        }
        if (adapter is ConcatAdapter) {
            if (adapter.findAndCountBefore(dest, count)) {
                return true
            }
        } else {
            count.set(count.get() + adapter.itemCount)
        }
    }
    return false
}

internal fun ConcatAdapter.getAfterCount(dest: RecyclerView.Adapter<*>?): Int {
    val count = AtomicInteger(0)
    if (findAndCountAfter(dest, count)) {
        return count.get()
    }
    return 0
}

internal fun ConcatAdapter.findAndCountAfter(dest: RecyclerView.Adapter<*>?, count: AtomicInteger): Boolean {
    for (adapter in this.adapters.reversed()) {
        if (adapter === dest) {
            return true
        }
        if (adapter is ConcatAdapter) {
            if (adapter.findAndCountAfter(dest, count)) {
                return true
            }
        } else {
            count.set(count.get() + adapter.itemCount)
        }
    }
    return false
}