package me.alibinkhani.stickyheaderapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import me.alibinkhani.stickyheaderapplication.databinding.StickyHeaderBinding
import me.alibinkhani.stickyheaderapplication.databinding.StickyHeaderItemBinding

class StickyHeaderAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val TYPE_ITEM = 1
        const val TYPE_STICKY_HEADER = 2
    }

    private var toast: Toast? = null

    override fun getItemViewType(position: Int): Int {
        return if (position % 10 == 0) TYPE_STICKY_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        when (viewType) {
            TYPE_ITEM -> {
                val binding = StickyHeaderItemBinding.inflate(inflater, parent, false)
                return ItemViewHolder(binding)
            }
            TYPE_STICKY_HEADER -> {
                val binding = StickyHeaderBinding.inflate(inflater, parent, false)
                return StickyViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val headPos = position / 10

        when (holder) {
            is ItemViewHolder -> onBindViewHolder(holder, position - headPos)
            is StickyViewHolder -> onBindViewHolder(holder, headPos)
        }
    }

    private fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.binding.textView.text = "Item $position"
    }

    private fun onBindViewHolder(holder: StickyViewHolder, position: Int) {
        holder.itemView.alpha = 0.5f
        holder.binding.textView.text = "Header $position"

        holder.itemView.setOnClickListener {
            this.toast?.cancel()
            Toast.makeText(it.context, "Header clicked! ${holder.bindingAdapterPosition}", Toast.LENGTH_SHORT).apply {
                toast = this
                show()
            }
        }
    }

    fun isStickyHeader(position: Int): Boolean {
        return getItemViewType(position) == TYPE_STICKY_HEADER
    }

    override fun getItemCount(): Int {
        return 200
    }

    inner class ItemViewHolder(val binding: StickyHeaderItemBinding): RecyclerView.ViewHolder(binding.root)
    inner class StickyViewHolder(val binding: StickyHeaderBinding): RecyclerView.ViewHolder(binding.root)
}