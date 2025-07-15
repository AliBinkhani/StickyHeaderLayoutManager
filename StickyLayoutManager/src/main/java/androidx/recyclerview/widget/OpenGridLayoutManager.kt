package androidx.recyclerview.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View

open class OpenGridLayoutManager : GridLayoutManager {
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    )

    constructor(context: Context?, spanCount: Int) : super(context, spanCount)

    constructor(context: Context?, spanCount: Int, orientation: Int, reverseLayout: Boolean) : super(
        context,
        spanCount,
        orientation,
        reverseLayout
    )

    public override fun findOneVisibleChild(
        fromIndex: Int,
        toIndex: Int,
        completelyVisible: Boolean,
        acceptPartiallyVisible: Boolean
    ): View? {
        return super.findOneVisibleChild(fromIndex, toIndex, completelyVisible, acceptPartiallyVisible)
    }
}
