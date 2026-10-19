package com.digitalsignage.dsnetcoreplayer.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout




class PlayerLayout : FrameLayout {
    private var x = 0
    private var y = 0
    private var width = 0
    private var height = 0

    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    )

    fun setDimensions(x: Int, y: Int, width: Int, height: Int) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        requestLayout()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        layout(x, y, x + width, y + height)
    }
}

