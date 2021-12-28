package com.yh.cxqr.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.View
import com.yh.cxqr.model.Barcode
import com.yh.cxqr.utils.DisplayUtils

internal class ResultView(context: Context?) : View(context) {

    private val pointSize = DisplayUtils.dp2px(context, 12).toFloat()

    private var resultBarcode: Barcode? = null
    private val previewRect = Rect()

    private val paint = Paint()

    init {
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        paint.alpha = 0xA0
        paint.color = Color.parseColor("#C0FFBD21")
    }

    override fun onDraw(canvas: Canvas?) {
        if (null == canvas) {
            return
        }
        resultBarcode?.also {
            val imageCropRect = it.imageCropRect
            val scaleX: Float = previewRect.width() / (imageCropRect.right + imageCropRect.left).toFloat()
            val scaleY: Float = previewRect.height() / (imageCropRect.bottom + imageCropRect.top).toFloat()
            Log.d("QRSV", "onDraw: $scaleX, $scaleY")

            val frameLeft = imageCropRect.left
            val frameTop = imageCropRect.top

            for (point in it.points) {
                canvas.drawCircle(
                    (frameLeft + (point.x * scaleX)),
                    (frameTop + (point.y * scaleY)),
                    pointSize, paint
                )
            }
        }
    }

    fun showResult(barcode: Barcode, rect: Rect) {
        Log.d("QRSV", "showResult: ${barcode.points.size} ${barcode.bottomLeft}, ${barcode.topRight}, ${barcode.imageCropRect}, $rect, $top,$left,$right,$bottom")
        resultBarcode = barcode
//        this.previewRect.set(top, left, right, bottom)
        this.previewRect.set(rect)
        postInvalidate()
//        postDelayed({
//            resultBarcode = null
//            previewRect.setEmpty()
//        }, 1000)
//        postInvalidateDelayed(5000)
    }

}