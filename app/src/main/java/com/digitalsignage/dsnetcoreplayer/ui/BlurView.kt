package com.digitalsignage.dsnetcoreplayer.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi

class BlurView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var blurRadius = 0f
    private val paint = Paint()

    init {
        setBackgroundColor(0x00FFFFFF) // Transparent background
    }

    fun setBlurRadius(radius: Float) {
        blurRadius = radius
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP))
        } else {
            paint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }
}
