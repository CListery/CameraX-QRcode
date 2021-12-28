package com.yh.cxqr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.yh.cxqr.model.Barcode
import com.yh.cxqr.utils.ImageUtils
import com.yh.cxqr.utils.copyPixels
import com.yh.cxqr.utils.rotate
import com.yh.cxqr.utils.toLuminance
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

class QRCodeParser(
    private val reader: Reader = MultiFormatReader().apply {
        setHints(QRCODE_HINTS)
    },
    private val parseExecutor: ExecutorService = Executors.newSingleThreadExecutor()
) {

    companion object {
        @JvmStatic
        private val QRCODE_HINTS =
            mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))

        @ExperimentalTime
        @JvmStatic
        internal fun TimedValue<Barcode?>.log() = Log.d(
            "QRCA", "Found result in ${duration.toString(TimeUnit.MILLISECONDS)}"
        )
    }

    @WorkerThread
    @androidx.camera.core.ExperimentalGetImage
    fun decodeImageProxy(
        imageProxy: ImageProxy,
        success: (barcode: Barcode) -> Unit,
        fail: () -> Unit
    ) = parseExecutor.submit {
        decodeImageProxyImpl(imageProxy)
            ?.also(success)
            ?: fail.invoke()
    }

    @ExperimentalTime
    @androidx.camera.core.ExperimentalGetImage
    fun decodeImageProxyWithTimed(
        imageProxy: ImageProxy,
        success: (barcode: Barcode) -> Unit,
        fail: () -> Unit
    ) = parseExecutor.submit {
        measureTimedValue { decodeImageProxyImpl(imageProxy) }
            .also { it.log() }
            .value
            ?.also(success)
            ?: fail.invoke()
    }

    fun decodeImageUri(
        context: Context,
        fileUri: Uri,
        success: (barcode: Barcode) -> Unit,
        fail: () -> Unit
    ) = parseExecutor.submit {
        decodeImageUriImpl(context, fileUri)
            ?.also(success)
            ?: fail.invoke()
    }

    @ExperimentalTime
    fun decodeImageUriWithTimed(
        context: Context, fileUri: Uri,
        success: (barcode: Barcode) -> Unit,
        fail: () -> Unit
    ) = parseExecutor.submit {
        measureTimedValue { decodeImageUriImpl(context, fileUri) }
            .also { it.log() }
            .value
            ?.also(success)
            ?: fail.invoke()
    }

    fun decodeBitmap(
        originBitmap: Bitmap,
        success: (barcode: Barcode) -> Unit,
        fail: () -> Unit
    ) = parseExecutor.submit {
        decodeBitmapImpl(originBitmap)
            ?.also(success)
            ?: fail.invoke()
    }

    @ExperimentalTime
    fun decodeBitmapWithTimed(
        originBitmap: Bitmap,
        success: (barcode: Barcode) -> Unit,
        fail: () -> Unit
    ) = parseExecutor.submit {
        measureTimedValue { decodeBitmapImpl(originBitmap) }
            .also { it.log() }
            .value
            ?.also(success)
            ?: fail.invoke()
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun decodeImageProxyImpl(imageProxy: ImageProxy): Barcode? {
        return kotlin.runCatching {
            val image = imageProxy.image ?: return@runCatching null
            val luminancePlane =
                image.toLuminance().rotate(imageProxy.imageInfo.rotationDegrees)
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
            return@runCatching Barcode.parse(reader.decode(bitmap), imageProxy)
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    private fun decodeImageUriImpl(context: Context, fileUri: Uri) =
        kotlin.runCatching {
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

    private fun decodeBitmapImpl(originBitmap: Bitmap) =
        kotlin.runCatching {
            val source = RGBLuminanceSource(
                originBitmap.width,
                originBitmap.height,
                originBitmap.copyPixels()
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            return@runCatching Barcode.parse(reader.decode(bitmap))
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
}