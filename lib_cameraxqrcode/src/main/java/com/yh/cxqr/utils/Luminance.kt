package com.yh.cxqr.utils

import android.graphics.Bitmap
import android.graphics.ImageFormat.YUV_420_888
import android.media.Image
import android.util.Log
import java.nio.ByteBuffer

class Luminance(val byteArray: ByteArray, val width: Int, val height: Int)

/* https://stackoverflow.com/a/58113173/3615879 */
fun Luminance.rotate(rotationDegrees: Int): Luminance {
    require(rotationDegrees % 90 == 0)
    if (rotationDegrees == 0) return this
    val newByteArray = ByteArray(byteArray.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            when (rotationDegrees) {
                90 -> newByteArray[x * height + height - y - 1] = byteArray[x + y * width] // Fill from top-right toward left (CW)
                180 -> newByteArray[width * (height - y - 1) + width - x - 1] = byteArray[x + y * width] // Fill from bottom-right toward up (CW)
                270 -> newByteArray[y + x * height] = byteArray[y * width + width - x - 1] // The opposite (CCW) of 90 degrees
            }
        }
    }
    val is90Flipped = rotationDegrees != 180
    val newWidth = if (is90Flipped) height else width
    val newHeight = if (is90Flipped) width else height
    return Luminance(byteArray = newByteArray, width = newWidth, height = newHeight)
}

/**
 * See [androidx.camera.core.ImageAnalysis.Analyzer.analyze] for the expected [android.graphics.ImageFormat].
 */
fun Image.toLuminance(): Luminance {
    require(format == YUV_420_888) { "Unexpected format, expected $YUV_420_888 but got $format instead." }
    return Luminance(byteArray = planes[0].buffer.toByteArray(), width = width, height = height)
}

fun Bitmap.toLuminance(): Luminance {
//    Log.d("Luminance", "toLuminance: $config")
//    return Luminance(byteArray = toYUV(), width = width, height = height)
    return Luminance(byteArray = toYUV2(), width = width, height = height)
}

private fun ByteBuffer.toByteArray(): ByteArray = ByteArray(capacity()).apply {
    if(position() != 0) {
        rewind()
    }
    get(this)
}
