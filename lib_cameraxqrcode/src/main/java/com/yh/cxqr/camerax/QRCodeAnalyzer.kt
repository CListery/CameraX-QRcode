package com.yh.cxqr.camerax

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.yh.cxqr.QRCodeParser
import com.yh.cxqr.model.Barcode
import kotlin.time.ExperimentalTime

internal class QRCodeAnalyzer(private val resultHandler: (barcode: Barcode?, nextBlock: () -> Unit) -> Unit) :
    ImageAnalysis.Analyzer {

    private val parser by lazy { QRCodeParser() }

    @ExperimentalTime
    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        parser.decodeImageProxyWithTimed(imageProxy, success = {
            resultHandler.invoke(it, imageProxy::close)
        }, fail = {
            imageProxy.close()
        })
    }
}