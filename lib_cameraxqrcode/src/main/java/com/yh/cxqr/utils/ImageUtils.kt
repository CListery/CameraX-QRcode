package com.yh.cxqr.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


object ImageUtils {

    @JvmStatic
    fun getImageFileRotate(inputStream: InputStream): Int {
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            val exifInterface = ExifInterface(inputStream)
            // 获取图片的旋转信息
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return 0
    }

    @JvmStatic
    fun getBitmapByUri(context: Context, uri: Uri): Bitmap? {
        context.contentResolver.openInputStream(uri)?.buffered()?.use { bufferIS ->
            return getBitmapByStream(bufferIS)
        }
        return null
    }

    @JvmStatic
    fun getBitmapByStream(inputStream: InputStream): Bitmap? {
        val options = BitmapFactory.Options()
        if (inputStream.markSupported()) {
            inputStream.mark(Int.MAX_VALUE)
            options.inJustDecodeBounds = true
            options.inSampleSize = 1
            BitmapFactory.decodeStream(inputStream, null, options)
            options.inSampleSize = computeSize(options, 400, 400)
            options.inJustDecodeBounds = false
            inputStream.reset()
        }
        return BitmapFactory.decodeStream(inputStream, null, options)
    }

    @JvmStatic
    fun computeSize(options: BitmapFactory.Options, outW: Int, outH: Int): Int {
        var inSampleSize = 1

        val srcWidth = options.outWidth
        val srcHeight = options.outHeight

        if (srcHeight > outH || srcWidth > outW) {
            val tmpW = if (outW % 2 == 1) outW + 1 else outW
            val tmpH = if (outH % 2 == 1) outH + 1 else outH
            val longSide = max(tmpW, tmpH)

            val heightRatio = (srcHeight.toFloat() / longSide.toFloat()).roundToInt()
            val widthRatio = (srcWidth.toFloat() / longSide.toFloat()).roundToInt()
            inSampleSize = max(heightRatio, widthRatio)
        }
        return inSampleSize
    }
}

fun Bitmap.toIntArray(): IntArray {
    val buffer = IntBuffer.allocate(byteCount)
    copyPixelsToBuffer(buffer)
    return buffer.toIntArray()
}

private fun IntBuffer.toIntArray(): IntArray = IntArray(capacity()).apply {
    if (position() != 0) {
        rewind()
    }
    get(this)
}

fun Bitmap.copyPixels(): IntArray {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    return pixels
}

fun Bitmap.toYUV(): ByteArray {
    return toY(copyPixels())
}

fun Bitmap.toYUV2(): ByteArray {
    return toY(toIntArray())
}

private fun Bitmap.toY(pixels: IntArray): ByteArray {
    val len = pixels.size
    val yuv = ByteArray(len * 3 / 2)
    var y: Int

    val mask = 0x00FFFFFF
    val byteMask = 0xFF

    for (row in 0 until height) {
        for (column in 0 until width) {
            val rgb = pixels[row * width + column] and mask

            val r = rgb and byteMask
            val g = rgb shr 8 and byteMask
            val b = rgb shr 16 and byteMask

            y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16

            y = if (y < 16) 16 else min(255, y)

            yuv[row * width + column] = y.toByte()
        }
    }
    return yuv
}
