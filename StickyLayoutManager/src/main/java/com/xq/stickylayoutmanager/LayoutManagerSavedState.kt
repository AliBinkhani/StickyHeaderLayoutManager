package com.xq.stickylayoutmanager

import android.os.Parcelable
import androidx.recyclerview.widget.RecyclerView
import kotlinx.parcelize.Parcelize

/**
 * Save / restore existing [RecyclerView] state and
 * scrolling position and offset.
 */
@Parcelize
data class LayoutManagerSavedState(
    val superState: Parcelable?,
    val scrollPosition: Int,
    val scrollOffset: Int
) : Parcelable