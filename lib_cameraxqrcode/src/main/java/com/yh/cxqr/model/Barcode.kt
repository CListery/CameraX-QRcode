package com.yh.cxqr.model

import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import androidx.core.net.toUri
import com.google.zxing.Result
import com.google.zxing.ResultPoint

typealias BarcodeCreator = () -> Barcode

sealed class Barcode {

    companion object {

        private const val KEY_TXT = "text"

        @JvmStatic
        private val TXT_CREATOR = { Text() }

        @JvmStatic
        private val barcodeFactory: HashMap<String, BarcodeCreator> = hashMapOf()

        @JvmStatic
        fun support(scheme: String, creator: BarcodeCreator) {
            barcodeFactory[scheme.uppercase()] = creator
        }

        init {
            support(KEY_TXT, TXT_CREATOR)
        }

        @JvmStatic
        fun parse(result: Result, imageProxy: ImageProxy? = null): Barcode {
            val value = result.text
            val valueUri = value?.toUri()
            val scheme = valueUri?.scheme?.uppercase()
            val creator = barcodeFactory[scheme] ?: TXT_CREATOR
            val barcode = creator.invoke()
            barcode.load(result, value, valueUri)
            // bottomLeft, topLeft, topRight
            result.resultPoints.forEachIndexed { index, rp ->
                when (index) {
                    0 -> barcode.bottomLeft.set(rp.x, rp.y)
                    1 -> barcode.topLeft.set(rp.x, rp.y)
                    2 -> barcode.topRight.set(rp.x, rp.y)
                }
            }
            barcode.points = result.resultPoints
            imageProxy?.cropRect?.also { barcode.imageCropRect.set(it) }
            return barcode
        }
    }

    open var value: String = ""
    open var format: Format = Format.UNKNOWN

    var timestamp = SystemClock.elapsedRealtime()

    internal var imageCropRect = Rect()
    internal var points: Array<ResultPoint> = emptyArray()
    internal val bottomLeft = PointF()
    internal val topLeft = PointF()
    internal val topRight = PointF()

    abstract val key: String

    open fun load(result: Result, value: String?, valueUri: Uri?) {
        this.format = Format.of(result)
        this.value = value ?: ""
    }

    class Text : Barcode() {
        override val key: String = KEY_TXT
    }

}