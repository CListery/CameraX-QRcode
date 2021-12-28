package com.yh.cxqr.camerax

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.yh.cxqr.model.Barcode
import com.yh.cxqr.utils.*
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.time.ExperimentalTime
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue


internal class QRCodeAnalyzer(private val resultHandler: (barcode: Barcode?, nextBlock: () -> Unit) -> Unit) :
    ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(QRCODE_HINTS)
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        imageProxy.runCatching {
            resultHandler.invoke(processImage(this), imageProxy::close)
        }.onFailure {
            it.printStackTrace()
            imageProxy.close()
        }
    }

    @WorkerThread
    @androidx.camera.core.ExperimentalGetImage
    private fun processImage(imageProxy: ImageProxy) = runBlocking { decode(imageProxy) }

    @WorkerThread
    @androidx.camera.core.ExperimentalGetImage
    fun decode(imageProxy: ImageProxy): Barcode? {
        return kotlin.runCatching {
            val image = imageProxy.image ?: return null
            val luminancePlane = image.toLuminance().rotate(imageProxy.imageInfo.rotationDegrees)
            val source = PlanarYUVLuminanceSource(
                luminancePlane.byteArray,
                luminancePlane.width,
                luminancePlane.height,
                0,
                0,
                luminancePlane.width,
                luminancePlane.height,
                false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            return Barcode.parse(reader.decode(bitmap), imageProxy)
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    @ExperimentalTime
    @androidx.camera.core.ExperimentalGetImage
    fun decodeWithTimed(imageProxy: ImageProxy): Barcode? =
        measureTimedValue { decode(imageProxy) }
            .also { it.log() }
            .value

    fun decodeImageUri(context: Context, fileUri: Uri): Barcode? {
        return kotlin.runCatching {
            return@runCatching context.contentResolver.openInputStream(fileUri)
                ?.buffered()
                ?.use { bis ->
                    val originBitmap = ImageUtils.getBitmapByStream(bis)
                        ?: throw NullPointerException("origin bitmap load fail!")
                    val source = RGBLuminanceSource(
                        originBitmap.width,
                        originBitmap.height,
                        originBitmap.copyPixels()
                    )
                    // 注释方法比较慢
//                val luminancePlane = originBitmap
//                    .toLuminance()
//                    .rotate(ImageUtils.getImageFileRotate(bis))
//                val source = PlanarYUVLuminanceSource(
//                    luminancePlane.byteArray,
//                    luminancePlane.width,
//                    luminancePlane.height,
//                    0,
//                    0,
//                    luminancePlane.width,
//                    luminancePlane.height,
//                    false
//                )
                    val bitmap = BinaryBitmap(HybridBinarizer(source))
                    return@use Barcode.parse(reader.decode(bitmap))
                }
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    @ExperimentalTime
    fun decodeImageUriWithTimed(context: Context, fileUri: Uri): Barcode? = measureTimedValue {
        decodeImageUri(context, fileUri)
    }.also {
        it.log()
    }.value

    fun decodeBitmap(originBitmap: Bitmap): Barcode? {
        val source = RGBLuminanceSource(
            originBitmap.width,
            originBitmap.height,
            originBitmap.copyPixels()
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val result = kotlin.runCatching {
            Barcode.parse(reader.decode(bitmap))
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
        return result
    }

    @ExperimentalTime
    fun decodeBitmapWithTimed(originBitmap: Bitmap): Barcode? =
        measureTimedValue { decodeBitmap(originBitmap) }
            .also { it.log() }
            .value

    companion object {

        @JvmStatic
        private val QRCODE_HINTS =
            mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))

        private fun ImageProxy.centerX(): Double {
            return when (imageInfo.rotationDegrees) {
                90, 270 -> height * 0.5
                else -> width * 0.5
            }
        }

        private fun ImageProxy.centerY(): Double {
            return when (imageInfo.rotationDegrees) {
                90, 270 -> width * 0.5
                else -> height * 0.5
            }
        }

        private fun ImageProxy.distance(rect: Rect): Double {
            val centerX = rect.exactCenterX().toDouble()
            val centerY = rect.exactCenterY().toDouble()
            return distance(centerX(), centerX, centerY(), centerY)
        }

        private fun ImageProxy.distance(resultPoints: Array<out ResultPoint>): Double {
            val centerX = resultPoints.map { it.x }.average()
            val centerY = resultPoints.map { it.y }.average()
            return distance(centerX(), centerX, centerY(), centerY)
        }

        private fun Bitmap.distance(resultPoints: Array<out ResultPoint>): Double {
            val centerX = resultPoints.map { it.x }.average()
            val centerY = resultPoints.map { it.y }.average()
            return distance(width * 0.5, centerX, height * 0.5, centerY)
        }

        private fun distance(x1: Double, x2: Double, y1: Double, y2: Double): Double {
            return kotlin.math.sqrt((x1 - x2).pow(2.0) + (y1 - y2).pow(2.0))
        }

        @ExperimentalTime
        internal fun TimedValue<Barcode?>.log() = Log.d(
            "QRCA", "Found result in ${duration.toString(TimeUnit.MILLISECONDS)}"
        )
    }

    fun destroy() {
//        reader.destroy()
    }

}