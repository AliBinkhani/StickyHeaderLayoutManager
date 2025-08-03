package com.xq.stickylayoutmanager

import android.content.Context
import android.graphics.PointF
import android.os.Parcelable
import android.view.View
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.OpenLinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import kotlin.math.max
import kotlin.math.min

class StickyHeadersLinearLayoutManager @JvmOverloads constructor(
    context: Context,
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false,
    stickReverse: Boolean = reverseLayout,
) : OpenLinearLayoutManager(context, orientation, reverseLayout) {

    constructor(
        context: Context,
        orientation: Int = RecyclerView.VERTICAL,
        reverseLayout: Boolean = false,
        stickReverse: Boolean = reverseLayout,
        stickyHeaderProvider: StickyHeaderProvider,
    ): this(context, orientation, reverseLayout, stickReverse) {
        this.stickyHeaderProvider = stickyHeaderProvider
    }

    private var adapter: RecyclerView.Adapter<*>? = null
    var stickyHeaderProvider: StickyHeaderProvider? = null

    /**
     * If true, the sticky header will be positioned at the bottom of the list, otherwise at the top.
     */
    var stickReverse: Boolean = stickReverse
        set(value) {
            field = value
            requestLayout()
        }

    /**
     * Offsets the horizontal location of the sticky header relative to the its default position.
     */
    var translationX = 0f
        set(value) {
            field = value
            requestLayout()
        }

    /**
     * Offsets the vertical location of the sticky header relative to the its default position.
     */
    var translationY = 0f
        set(value) {
            field = value
            requestLayout()
        }

    // Header positions for the currently displayed list and their observer.
    private val headerPositions: MutableList<Int> = ArrayList(0)
    private val headerPositionsObserver: AdapterDataObserver = HeaderPositionsAdapterDataObserver()

    // Sticky header's ViewHolder and dirty state.
    private var currentStickyHeader: View? = null
    private var currentStickyHeaderPosition = RecyclerView.NO_POSITION
    private var lastStickyHeaderOriginalView: View? = null

    private var pendingScrollPosition = RecyclerView.NO_POSITION
    private var pendingScrollOffset = 0

    var scrollEnabled = true

    override fun canScrollHorizontally(): Boolean {
        return super.canScrollHorizontally() && scrollEnabled
    }

    override fun canScrollVertically(): Boolean {
        return super.canScrollVertically() && scrollEnabled
    }

    /**
     * Returns true if `view` is the current sticky header.
     */
    fun isStickyHeader(view: View?): Boolean {
        return view === currentStickyHeader
    }

    override fun onAttachedToWindow(view: RecyclerView) {
        super.onAttachedToWindow(view)
        setAdapter(view.adapter)
    }

    override fun onAdapterChanged(oldAdapter: RecyclerView.Adapter<*>?, newAdapter: RecyclerView.Adapter<*>?) {
        super.onAdapterChanged(oldAdapter, newAdapter)
        setAdapter(newAdapter)
    }

    private fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
        this.adapter?.unregisterAdapterDataObserver(headerPositionsObserver)
        this.adapter = adapter
        this.adapter?.registerAdapterDataObserver(headerPositionsObserver)
        headerPositionsObserver.onChanged()
    }

    override fun onSaveInstanceState(): Parcelable? {
        return super.onSaveInstanceState()?.let {
            LayoutManagerSavedState(
                superState = it,
                scrollPosition = pendingScrollPosition,
                scrollOffset = pendingScrollOffset
            )
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        state as LayoutManagerSavedState
        pendingScrollPosition = state.scrollPosition
        pendingScrollOffset = state.scrollOffset
        super.onRestoreInstanceState(state.superState)
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State?): Int {
        detachStickyHeader()
        val scrolled = super.scrollVerticallyBy(dy, recycler, state)
        attachStickyHeader()

        if (scrolled != 0) {
            updateStickyHeader(recycler, false)
        }

        return scrolled
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State?): Int {
        detachStickyHeader()
        val scrolled = super.scrollHorizontallyBy(dx, recycler, state)
        attachStickyHeader()

        if (scrolled != 0) {
            updateStickyHeader(recycler, false)
        }

        return scrolled
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        detachStickyHeader()
        super.onLayoutChildren(recycler, state)
        attachStickyHeader()

        if (!state.isPreLayout) {
            updateStickyHeader(recycler, true)
        }
    }

    override fun scrollToPosition(position: Int) {
        scrollToPositionWithOffset(position, INVALID_OFFSET)
    }

    override fun scrollToPositionWithOffset(position: Int, offset: Int) {
        scrollToPositionWithOffset(position, offset, true)
    }

    private fun scrollToPositionWithOffset(position: Int, offset: Int, adjustForStickyHeader: Boolean) {
        // Reset pending scroll.
        setPendingScroll(RecyclerView.NO_POSITION, INVALID_OFFSET)

        // Adjusting is disabled.
        if (!adjustForStickyHeader) {
            super.scrollToPositionWithOffset(position, offset)
            return
        }

        // There is no header above or the position is a header.
        val headerIndex = findHeaderIndexOrBefore(position)
        if (headerIndex == -1 || findHeaderIndex(position) != -1) {
            super.scrollToPositionWithOffset(position, offset)
            return
        }

        // The position is right below a header, scroll to the header.
        if (findHeaderIndex(position - 1) != -1) {
            super.scrollToPositionWithOffset(position - 1, offset)
            return
        }

        // Current sticky header is the same as at the position. Adjust the scroll offset and reset pending scroll.
        if (currentStickyHeader != null && headerIndex == findHeaderIndex(currentStickyHeaderPosition)) {
            val adjustedOffset = (if (offset != INVALID_OFFSET) offset else 0) + currentStickyHeader!!.height
            super.scrollToPositionWithOffset(position, adjustedOffset)
            return
        }

        // Remember this position and offset and scroll to it to trigger creating the sticky header.
        setPendingScroll(position, offset)
        super.scrollToPositionWithOffset(position, offset)
    }

    override fun computeVerticalScrollExtent(state: RecyclerView.State): Int {
        detachStickyHeader()
        val extent = super.computeVerticalScrollExtent(state)
        attachStickyHeader()
        return extent
    }

    override fun computeVerticalScrollOffset(state: RecyclerView.State): Int {
        detachStickyHeader()
        val offset = super.computeVerticalScrollOffset(state)
        attachStickyHeader()
        return offset
    }

    override fun computeVerticalScrollRange(state: RecyclerView.State): Int {
        detachStickyHeader()
        val range = super.computeVerticalScrollRange(state)
        attachStickyHeader()
        return range
    }

    override fun computeHorizontalScrollExtent(state: RecyclerView.State): Int {
        detachStickyHeader()
        val extent = super.computeHorizontalScrollExtent(state)
        attachStickyHeader()
        return extent
    }

    override fun computeHorizontalScrollOffset(state: RecyclerView.State): Int {
        detachStickyHeader()
        val offset = super.computeHorizontalScrollOffset(state)
        attachStickyHeader()
        return offset
    }

    override fun computeHorizontalScrollRange(state: RecyclerView.State): Int {
        detachStickyHeader()
        val range = super.computeHorizontalScrollRange(state)
        attachStickyHeader()
        return range
    }

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        detachStickyHeader()
        val vector = super.computeScrollVectorForPosition(targetPosition)
        attachStickyHeader()
        return vector
    }

    override fun findFirstVisibleItemPosition(): Int {
        for (curPosition in 0..<childCount) {
            val child = findOneVisibleChild(
                fromIndex = curPosition,
                toIndex = childCount,
                completelyVisible = false,
                acceptPartiallyVisible = true
            )
            if (child != null && !isStickyHeader(child)) {
                return getPosition(child)
            }
        }
        return RecyclerView.NO_POSITION
    }

    override fun findFirstCompletelyVisibleItemPosition(): Int {
        for (curPosition in 0..<childCount) {
            val child = findOneVisibleChild(
                fromIndex = curPosition,
                toIndex = childCount,
                completelyVisible = true,
                acceptPartiallyVisible = false
            )
            if (child != null && !isStickyHeader(child)) {
                return getPosition(child)
            }
        }
        return RecyclerView.NO_POSITION
    }

    override fun findLastVisibleItemPosition(): Int {
        for (curPosition in childCount - 1 downTo 0) {
            val child = findOneVisibleChild(
                fromIndex = curPosition,
                toIndex = -1,
                completelyVisible = false,
                acceptPartiallyVisible = true
            )
            if (child != null && !isStickyHeader(child)) {
                return getPosition(child)
            }
        }
        return RecyclerView.NO_POSITION
    }

    override fun findLastCompletelyVisibleItemPosition(): Int {
        for (curPosition in childCount - 1 downTo 0) {
            val child = findOneVisibleChild(
                fromIndex = curPosition,
                toIndex = -1,
                completelyVisible = true,
                acceptPartiallyVisible = false
            )
            if (child != null && !isStickyHeader(child)) {
                return getPosition(child)
            }
        }
        return RecyclerView.NO_POSITION
    }

    override fun onFocusSearchFailed(
        focused: View,
        focusDirection: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): View? {
        detachStickyHeader()
        val view = super.onFocusSearchFailed(focused, focusDirection, recycler, state)
        attachStickyHeader()
        return view
    }

    private fun detachStickyHeader() {
        val stickyHeader: View? = currentStickyHeader
        if (stickyHeader != null) {
            detachView(stickyHeader)
        }
    }

    private fun attachStickyHeader() {
        val stickyHeader: View? = currentStickyHeader
        if (stickyHeader != null) {
            attachView(stickyHeader)
        }
    }

    /**
     * Updates the sticky header state (creation, binding, display), to be called whenever there's a layout or scroll
     */
    private fun updateStickyHeader(recycler: RecyclerView.Recycler, layout: Boolean) {
        val headerCount = headerPositions.size
        val childCount = getChildCount()
        if (headerCount > 0 && childCount > 0) {
            // Find first valid child.
            var anchorView: View? = null
            var anchorIndex = -1
            var anchorPos = -1

            // TRUE if exactly one of them is true, FALSE if both are same
            val isReversed = reverseLayout xor stickReverse
            val indices = if (isReversed)
                IntProgression.fromClosedRange(childCount - 1, 0, -1)
            else
                IntProgression.fromClosedRange(0, childCount - 1, 1)

            for (i in indices) {
                val child = getChildAt(i)
                val params = child!!.layoutParams as RecyclerView.LayoutParams
                if (!isStickyHeader(child) && isViewValidAnchor(child, params)) {
                    anchorView = child
                    anchorIndex = i
                    anchorPos = params.absoluteAdapterPosition
                    break
                }
            }

            if (anchorView != null && anchorPos != -1) {
                val headerPos: Int
                val nextHeaderPos: Int
                if (isReversed) {
                    val headerIndex = findHeaderIndexOrNext(anchorPos)
                    headerPos = if (headerIndex != -1) headerPositions[headerIndex] else -1
                    nextHeaderPos = headerPositions.getOrNull(headerIndex - 1) ?: -1
                } else {
                    val headerIndex = findHeaderIndexOrBefore(anchorPos)
                    headerPos = if (headerIndex != -1) headerPositions[headerIndex] else -1
                    nextHeaderPos = headerPositions.getOrNull(headerIndex + 1) ?: -1
                }

                // Show sticky header if:
                // - There's one to show;
                // - It's on the edge or it's not the anchor view;
                // - Isn't followed by another sticky header;
                if (headerPos != -1 && (headerPos != anchorPos || isViewOnBoundary(anchorView))
                    && nextHeaderPos != headerPos + 1
                ) {
                    var stickyHeader: View? = currentStickyHeader

                    // Ensure existing sticky header, if any, is of correct type.
                    if (stickyHeader != null
                        && getItemViewType(stickyHeader) != adapter?.getItemViewType(headerPos)
                    ) {
                        // A sticky header was shown before but is not of the correct type. Scrap it.
                        scrapStickyHeader(recycler)
                        stickyHeader = null
                    }

                    // Ensure sticky header is created, if absent, or bound, if being laid out or the position changed.
                    if (stickyHeader == null) {
                        createStickyHeader(recycler, headerPos)
                        stickyHeader = currentStickyHeader!!
                    }

                    if (layout || getPosition(stickyHeader) != headerPos) {
                        bindStickyHeader(recycler, headerPos, stickyHeader)
                    }

                    // Draw the sticky header using translation values which depend on orientation, direction and
                    // position of the next header view.
                    var nextHeaderView: View? = null
                    if (nextHeaderPos != -1) {
                        nextHeaderView = getChildAt(anchorIndex + (nextHeaderPos - anchorPos))
                        // The header view itself is added to the RecyclerView. Discard it if it comes up.
                        if (nextHeaderView === stickyHeader) {
                            nextHeaderView = null
                        }
                    }
                    stickyHeader.translationX = getX(stickyHeader, nextHeaderView)
                    stickyHeader.translationY = getY(stickyHeader, nextHeaderView)

                    // Hide the original header view if present
                    lastStickyHeaderOriginalView?.visibility = View.VISIBLE // Restore previous
                    if (getPosition(anchorView) == headerPos) {
                        anchorView.visibility = View.INVISIBLE
                        lastStickyHeaderOriginalView = anchorView
                    }
                    return
                }
            }
        }

        if (currentStickyHeader != null) {
            scrapStickyHeader(recycler)
            lastStickyHeaderOriginalView?.visibility = View.VISIBLE
        }
    }

    /**
     * Creates [RecyclerView.ViewHolder] for `position`, including measure / layout, and assigns it to
     * [.mStickyHeader].
     */
    private fun createStickyHeader(recycler: RecyclerView.Recycler, position: Int) {
        val stickyHeader = recycler.getViewForPosition(position)

        // Setup sticky header if the adapter requires it.
        val adapterPositionPair = adapter?.offsetPositionOnAdapter(position)
        (adapterPositionPair?.first as? OnViewStickyListener?)?.setupStickyHeaderView(stickyHeader)

        // Add sticky header as a child view, to be detached / reattached whenever LinearLayoutManager#fill() is called,
        // which happens on layout and scroll (see overrides).
        addView(stickyHeader)
        measureAndLayout(stickyHeader)

        // Ignore sticky header, as it's fully managed by this LayoutManager.
        ignoreView(stickyHeader)

        currentStickyHeader = stickyHeader
        currentStickyHeaderPosition = position
    }

    /**
     * Binds the [.mStickyHeader] for the given `position`.
     */
    private fun bindStickyHeader(recycler: RecyclerView.Recycler, position: Int, stickyHeader: View) {
        // Bind the sticky header.
        recycler.bindViewToPosition(stickyHeader, position)
        currentStickyHeaderPosition = position
        measureAndLayout(stickyHeader)

        // If we have a pending scroll wait until the end of layout and scroll again.
        if (pendingScrollPosition != RecyclerView.NO_POSITION) {
            val vto = stickyHeader.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    vto.removeOnGlobalLayoutListener(this)

                    if (pendingScrollPosition != RecyclerView.NO_POSITION) {
                        scrollToPositionWithOffset(pendingScrollPosition, pendingScrollOffset)
                        setPendingScroll(RecyclerView.NO_POSITION, INVALID_OFFSET)
                    }
                }
            })
        }
    }

    /**
     * Measures and lays out `stickyHeader`.
     */
    private fun measureAndLayout(stickyHeader: View) {
        measureChildWithMargins(stickyHeader, 0, 0)
        if (orientation == VERTICAL) {
            stickyHeader.layout(paddingLeft, 0, width - paddingRight, stickyHeader.measuredHeight)
        } else {
            stickyHeader.layout(0, paddingTop, stickyHeader.measuredWidth, height - paddingBottom)
        }
    }

    /**
     * Returns [.mStickyHeader] to the [RecyclerView]'s [RecyclerView.RecycledViewPool], assigning it
     * to `null`.
     *
     * @param recycler If passed, the sticky header will be returned to the recycled view pool.
     */
    private fun scrapStickyHeader(recycler: RecyclerView.Recycler?) {
        val stickyHeader = currentStickyHeader!!
        val stickyHeaderPosition = currentStickyHeaderPosition
        currentStickyHeader = null
        currentStickyHeaderPosition = RecyclerView.NO_POSITION

        // Revert translation values.
        stickyHeader.translationX = 0f
        stickyHeader.translationY = 0f

        // Teardown holder if the adapter requires it.
        val adapterPositionPair = adapter?.offsetPositionOnAdapter(stickyHeaderPosition)
        (adapterPositionPair?.first as? OnViewStickyListener?)?.teardownStickyHeaderView(stickyHeader)

        // Stop ignoring sticky header so that it can be recycled.
        stopIgnoringView(stickyHeader)

        // Remove and recycle sticky header.
        removeView(stickyHeader)
        recycler?.recycleView(stickyHeader)
    }

    /**
     * Returns true when `view` is a valid anchor, ie. the first view to be valid and visible.
     */
    private fun isViewValidAnchor(view: View, params: RecyclerView.LayoutParams): Boolean {
        if (!params.isItemRemoved && !params.isViewInvalid) {
            return if (orientation == VERTICAL) {
                if (reverseLayout) {
                    view.top + view.translationY <= height + translationY
                } else {
                    view.bottom - view.translationY >= translationY
                }
            } else {
                if (reverseLayout) {
                    view.left + view.translationX <= width + translationX
                } else {
                    view.right - view.translationX >= translationX
                }
            }
        } else {
            return false
        }
    }

    /**
     * Returns true when the `view` is at the edge of the parent [RecyclerView].
     */
    private fun isViewOnBoundary(view: View): Boolean {
        return if (orientation == VERTICAL) {
            if (stickReverse) {
                view.bottom - view.translationY > height + translationY
            } else {
                view.top + view.translationY < translationY
            }
        } else {
            if (stickReverse) {
                view.right - view.translationX > width + translationX
            } else {
                view.left + view.translationX < translationX
            }
        }
    }

    /**
     * Returns the position in the Y axis to position the header appropriately, depending on orientation, direction and
     * [android.R.attr.clipToPadding].
     */
    private fun getY(headerView: View, nextHeaderView: View?): Float {
        if (orientation != VERTICAL)
            return translationY

        return if (stickReverse) {
            val y = translationY + (height - headerView.height)
            if (nextHeaderView != null) {
                max(nextHeaderView.bottom.toFloat(), y)
            } else {
                y
            }
        } else {
            if (nextHeaderView != null) {
                min((nextHeaderView.top - headerView.height).toFloat(), translationY)
            } else {
                translationY
            }
        }
    }

    /**
     * Returns the position in the X axis to position the header appropriately, depending on orientation, direction and
     * [android.R.attr.clipToPadding].
     */
    private fun getX(headerView: View, nextHeaderView: View?): Float {
        if (orientation != HORIZONTAL)
            return translationX

        return if (stickReverse) {
            val x = translationX + (width - headerView.width)
            if (nextHeaderView != null) {
                max(nextHeaderView.right.toFloat(), x)
            } else {
                x
            }
        } else {
            if (nextHeaderView != null) {
                min((nextHeaderView.left - headerView.width).toFloat(), translationX)
            } else {
                translationX
            }
        }
    }

    /**
     * Finds the header index of `position` in `mHeaderPositions`.
     */
    private fun findHeaderIndex(position: Int): Int {
        var low = 0
        var high = headerPositions.size - 1
        while (low <= high) {
            val middle = (low + high) / 2
            if (headerPositions[middle] > position) {
                high = middle - 1
            } else if (headerPositions[middle] < position) {
                low = middle + 1
            } else {
                return middle
            }
        }
        return -1
    }

    /**
     * Finds the header index of `position` or the one before it in `mHeaderPositions`.
     */
    private fun findHeaderIndexOrBefore(position: Int): Int {
        var low = 0
        var high = headerPositions.size - 1
        while (low <= high) {
            val middle = (low + high) / 2
            if (headerPositions[middle] > position) {
                high = middle - 1
            } else if (middle < headerPositions.size - 1 && headerPositions[middle + 1] <= position) {
                low = middle + 1
            } else {
                return middle
            }
        }
        return -1
    }

    /**
     * Finds the header index of `position` or the one next to it in `mHeaderPositions`.
     */
    private fun findHeaderIndexOrNext(position: Int): Int {
        var low = 0
        var high = headerPositions.size - 1
        while (low <= high) {
            val middle = (low + high) / 2
            if (middle > 0 && headerPositions[middle - 1] >= position) {
                high = middle - 1
            } else if (headerPositions[middle] < position) {
                low = middle + 1
            } else {
                return middle
            }
        }
        return -1
    }

    private fun setPendingScroll(position: Int, offset: Int) {
        pendingScrollPosition = position
        pendingScrollOffset = offset
    }

    /**
     * Handles header positions while adapter changes occur.
     *
     *
     * This is used in detriment of [RecyclerView.LayoutManager]'s callbacks to control when they're received.
     */
    private inner class HeaderPositionsAdapterDataObserver : AdapterDataObserver() {
        override fun onChanged() {
            // There's no hint at what changed, so go through the adapter.
            headerPositions.clear()

            val adapter = adapter
            val stickyHeaderProvider = stickyHeaderProvider
            if (adapter != null && stickyHeaderProvider != null) {
                val itemCount = adapter.itemCount
                for (i in 0..<itemCount) {
                    val (adapter, position) = adapter.offsetPositionOnAdapter(i)
                    if (stickyHeaderProvider.isStickyHeader(adapter, position)) {
                        headerPositions.add(i)
                    }
                }
            }

            // Remove sticky header immediately if the entry it represents has been removed. A layout will follow.
            if (currentStickyHeader != null && !headerPositions.contains(currentStickyHeaderPosition)) {
                scrapStickyHeader(null)
            }
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            // Shift headers below down.
            val headerCount = headerPositions.size
            if (headerCount > 0) {
                var i = findHeaderIndexOrNext(positionStart)
                while (i != -1 && i < headerCount) {
                    headerPositions[i] = headerPositions[i] + itemCount
                    i++
                }
            }

            // Add new headers.
            val adapter = adapter
            val stickyHeaderProvider = stickyHeaderProvider
            if (adapter != null && stickyHeaderProvider != null) {
                for (i in positionStart..<positionStart + itemCount) {
                    val (adapter, position) = adapter.offsetPositionOnAdapter(i)
                    if (stickyHeaderProvider.isStickyHeader(adapter, position)) {
                        val headerIndex = findHeaderIndexOrNext(i)
                        if (headerIndex != -1) {
                            headerPositions.add(headerIndex, i)
                        } else {
                            headerPositions.add(i)
                        }
                    }
                }
            }
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            var headerCount = headerPositions.size
            if (headerCount > 0) {
                // Remove headers.
                for (i in positionStart + itemCount - 1 downTo positionStart) {
                    val index = findHeaderIndex(i)
                    if (index != -1) {
                        headerPositions.removeAt(index)
                        headerCount--
                    }
                }

                // Remove sticky header immediately if the entry it represents has been removed. A layout will follow.
                if (currentStickyHeader != null && !headerPositions.contains(currentStickyHeaderPosition)) {
                    scrapStickyHeader(null)
                }

                // Shift headers below up.
                var i = findHeaderIndexOrNext(positionStart + itemCount)
                while (i != -1 && i < headerCount) {
                    headerPositions[i] = headerPositions[i] - itemCount
                    i++
                }
            }
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            // Shift moved headers by toPosition - fromPosition.
            // Shift headers in-between by -itemCount (reverse if upwards).
            val headerCount = headerPositions.size
            if (headerCount > 0) {
                if (fromPosition < toPosition) {
                    var i = findHeaderIndexOrNext(fromPosition)
                    while (i != -1 && i < headerCount) {
                        val headerPos = headerPositions[i]
                        if (headerPos >= fromPosition && headerPos < fromPosition + itemCount) {
                            headerPositions[i] = headerPos - (toPosition - fromPosition)
                            sortHeaderAtIndex(i)
                        } else if (headerPos >= fromPosition + itemCount && headerPos <= toPosition) {
                            headerPositions[i] = headerPos - itemCount
                            sortHeaderAtIndex(i)
                        } else {
                            break
                        }
                        i++
                    }
                } else {
                    var i = findHeaderIndexOrNext(toPosition)
                    while (i != -1 && i < headerCount) {
                        val headerPos = headerPositions[i]
                        if (headerPos >= fromPosition && headerPos < fromPosition + itemCount) {
                            headerPositions[i] = headerPos + (toPosition - fromPosition)
                            sortHeaderAtIndex(i)
                        } else if (headerPos >= toPosition && headerPos <= fromPosition) {
                            headerPositions[i] = headerPos + itemCount
                            sortHeaderAtIndex(i)
                        } else {
                            break
                        }
                        i++
                    }
                }
            }
        }

        fun sortHeaderAtIndex(index: Int) {
            val headerPos = headerPositions.removeAt(index)
            val headerIndex = findHeaderIndexOrNext(headerPos)
            if (headerIndex != -1) {
                headerPositions.add(headerIndex, headerPos)
            } else {
                headerPositions.add(headerPos)
            }
        }
    }
}
