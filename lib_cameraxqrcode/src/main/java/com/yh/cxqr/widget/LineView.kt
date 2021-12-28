package com.yh.cxqr.widget

import android.content.Context
import android.graphics.*
import android.view.View
import android.view.ViewTreeObserver
import com.yh.cxqr.utils.DisplayUtils
import kotlin.math.abs

internal class LineView(context: Context?) : View(context) {

    private var scanLineMarginX = 0F
    private var scanLineMarginY = 0F
    private var scanLineTop = 0F
    private var scanLineH = DisplayUtils.dp2px(context, 4)
    private var scanLineMoveDistance = DisplayUtils.dp2px(context, 2)

    private val scanLinePaint = Paint()

    init {
        scanLinePaint.style = Paint.Style.FILL
        scanLinePaint.isAntiAlias = true
        scanLinePaint.strokeWidth = scanLineH.toFloat()
        scanLinePaint.color = Color.parseColor("#00FF77")

        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)

                scanLineMarginX = measuredWidth * 0.1F
                scanLineMarginY = measuredHeight * 0.1F
                scanLineTop = scanLineMarginY

                scanLinePaint.shader = LinearGradient(
                    0F + scanLineMarginX,
                    0F,
                    measuredWidth.toFloat() - scanLineMarginX,
                    0F,
                    intArrayOf(
                        Color.parseColor("#0000FF77"),
                        Color.parseColor("#9900FF77"),
                        Color.parseColor("#0000FF77"),
                    ),
                    floatArrayOf(
                        0F,
                        0.5F,
                        1F
                    ),
                    Shader.TileMode.CLAMP
                )
            }
        }
        viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    override fun onDetachedFromWindow() {
        scanLineTop = scanLineMarginY
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas?) {
        if (null == canvas) {
            return
        }
        val max = bottom / 2
        scanLinePaint.alpha = (255 * (max - abs(scanLineTop - max)) / max).toInt()

        canvas.drawLine(
            left.toFloat() + scanLineMarginX,
            scanLineTop,
            right.toFloat() - scanLineMarginX,
            scanLineTop,
            scanLinePaint
        )

        moveScanLine()
    }

    private fun moveScanLine() {
        if (scanLineTop >= bottom - scanLineMarginY * 2) {
            scanLineTop = top + scanLineMarginY
        } else {
            scanLineTop += scanLineMoveDistance
        }
        postInvalidateDelayed(
            10L,
            left,
            top,
            right,
            bottom
        )
    }

}