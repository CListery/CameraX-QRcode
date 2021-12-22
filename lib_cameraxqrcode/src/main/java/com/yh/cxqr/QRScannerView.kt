package com.yh.cxqr

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.camera.core.*
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.yh.cxqr.camerax.QRCodeAnalyzer
import com.yh.cxqr.model.Barcode
import com.yh.cxqr.utils.DisplayUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class QRScannerView : FrameLayout {
    private companion object {
        private const val TAG = "QRSV"

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    var resultCallback: IScanResultCallback? = null

    private val cc = LifecycleCameraController(context)

    private val preView: PreviewView = PreviewView(context)
    private val singleTapGestureDetector by lazy {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                if (null == e) {
                    return false
                }
                startFocusAnim(e.x.toInt(), e.y.toInt())
                return true
            }
        })
    }

    private val focusTouchListener by lazy {
        @Suppress("ClickableViewAccessibility")
        OnTouchListener { _, event -> singleTapGestureDetector.onTouchEvent(event) }
    }

    private val mainExecutor = ContextCompat.getMainExecutor(context)

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val qrRCodeAnalyzer: QRCodeAnalyzer = QRCodeAnalyzer(this::onQRCodeAnalysisResult)

    private val lineView: LineView = LineView(context)
    private val resultView: ResultView = ResultView(context)
    private val focusView: ImageView = ImageView(context)
    private val focusSize = DisplayUtils.dp2px(context, 60)
    private val focusAnim by lazy {
        AnimationUtils.loadAnimation(context, R.anim.focus_anim).also {
            it.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {

                }

                override fun onAnimationEnd(animation: Animation) {
                    focusView.visibility = View.INVISIBLE
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
        }
    }

    var isScanning = false

    private var rotation = -1
    private val preRect = Rect()
    private var screenAspectRatio = -1

    init {
        addView(preView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(lineView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        lineView.visibility = View.GONE
        addView(focusView, LayoutParams(focusSize, focusSize))
        addView(resultView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        focusView.visibility = View.INVISIBLE
        focusView.setImageResource(R.drawable.icon_focus)

        setOnTouchListener(focusTouchListener)

        doOnAttach {
            Log.d(TAG, "onAttach")

            loadDisplayInfo()

            findViewTreeLifecycleOwner()!!.runCatching {
                cc.bindToLifecycle(this)

                initCameraController()

                preView.controller = cc

                cc.isTapToFocusEnabled = true

                cc.zoomState.observe(this, object : Observer<ZoomState> {
                    override fun onChanged(t: ZoomState?) {
                        cc.zoomState.removeObserver(this)
                        observeCameraState(cc.cameraInfo, this@runCatching)
                    }
                })
            }

            doOnDetach {
                Log.d(TAG, "onDetach")
                stopScan(false)
                cameraExecutor.shutdown()
                qrRCodeAnalyzer.destroy()
                cc.unbind()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        loadDisplayInfo()
    }

    private fun loadDisplayInfo() {
        val windowManager = context.getSystemService<WindowManager>()
        rotation = (preView.display ?: display).rotation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager?.currentWindowMetrics?.bounds?.also(preRect::set)
        } else {
            windowManager?.defaultDisplay?.getRectSize(preRect)
        }
        screenAspectRatio = aspectRatio(preRect.width(), preRect.height())
        Log.d(
            TAG,
            "loadDisplayInfo: [${preRect.width()} x ${preRect.height()}], $rotation, $screenAspectRatio"
        )
    }

    fun switchFlash() {
        when (cc.torchState.value) {
            TorchState.ON -> cc.enableTorch(false)
            TorchState.OFF -> cc.enableTorch(true)
        }
    }

    fun beginScan() {
        if (isScanning) {
            return
        }
        isScanning = true

        preView.controller = cc

        lineView.visibility = View.VISIBLE

        cc.clearImageAnalysisAnalyzer()

        cc.setImageAnalysisAnalyzer(cameraExecutor, qrRCodeAnalyzer)
    }

    @JvmOverloads
    fun stopScan(resetZoom: Boolean = true) {
        if (isScanning) {
            lineView.visibility = View.GONE

            cc.clearImageAnalysisAnalyzer()
            if (resetZoom) {
                cc.zoomState.value?.minZoomRatio?.also {
                    cc.setZoomRatio(it)
                }
            } else {
//                preView.controller = null
            }
            isScanning = false
        }
    }

    private fun initCameraController() {
        cc.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cc.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
    }

    private fun onQRCodeAnalysisResult(barcode: Barcode?, nextBlock: () -> Unit) {
        Log.d(TAG, "onQRCodeAnalysisResult: $barcode")
        if (null == barcode) {
            nextBlock.invoke()
            return
        }
        mainExecutor.execute {
            stopScan(false)
            nextBlock.invoke()
//            val rect = Rect()
//            preView.getLocalVisibleRect(rect)
//            resultView.showResult(barcode, rect)
            resultCallback?.onCallback(barcode)
        }
    }

    private fun startFocusAnim(x: Int, y: Int) {
        if (focusView.isShown) {
            return
        }

        mainExecutor.execute {
            focusView.visibility = View.VISIBLE
            focusView.layout(
                x - focusSize / 2,
                y - focusSize / 2,
                x + focusSize / 2,
                y + focusSize / 2
            )
            focusView.startAnimation(focusAnim)
        }
    }

    /**
     *  [androidx.camera.core.ImageAnalysis.Builder] requires enum value of
     *  [androidx.camera.core.AspectRatio.Ratio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun observeCameraState(cameraInfo: CameraInfo?, viewLifecycleOwner: LifecycleOwner) {
        if (null == cameraInfo) {
            return
        }
        cameraInfo.cameraState.observe(viewLifecycleOwner) { cameraState ->
            run {
                when (cameraState.type) {
                    CameraState.Type.PENDING_OPEN -> {
                        // Ask the user to close other camera apps
                        Log.d(TAG, "CameraState: Pending Open")
                    }
                    CameraState.Type.OPENING -> {
                        // Show the Camera UI
                        Log.d(TAG, "CameraState: Opening")
                    }
                    CameraState.Type.OPEN -> {
                        // Setup Camera resources and begin processing
                        Log.d(TAG, "CameraState: Open")
                    }
                    CameraState.Type.CLOSING -> {
                        // Close camera UI
                        Log.d(TAG, "CameraState: Closing")
                    }
                    CameraState.Type.CLOSED -> {
                        // Free camera resources
                        Log.d(TAG, "CameraState: Closed")
                    }
                }
            }

            cameraState.error?.let { error ->
                when (error.code) {
                    // Open errors
                    CameraState.ERROR_STREAM_CONFIG -> {
                        // Make sure to setup the use cases properly
                        Log.e(TAG, "Stream config error")
                    }
                    // Opening errors
                    CameraState.ERROR_CAMERA_IN_USE -> {
                        // Close the camera or ask user to close another camera app that's using the
                        // camera
                        Log.e(TAG, "Camera in use")
                    }
                    CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                        // Close another open camera in the app, or ask the user to close another
                        // camera app that's using the camera
                        Log.e(TAG, "Max cameras in use")
                    }
                    CameraState.ERROR_OTHER_RECOVERABLE_ERROR -> {
                        Log.e(TAG, "Other recoverable error")
                    }
                    // Closing errors
                    CameraState.ERROR_CAMERA_DISABLED -> {
                        // Ask the user to enable the device's cameras
                        Log.e(TAG, "Camera disabled")
                    }
                    CameraState.ERROR_CAMERA_FATAL_ERROR -> {
                        // Ask the user to reboot the device to restore camera function
                        Log.e(TAG, "Fatal error")
                    }
                    // Closed errors
                    CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                        // Ask the user to disable the "Do Not Disturb" mode, then reopen the camera
                        Log.e(TAG, "Do not disturb mode enabled")
                    }
                }
            }
        }
    }

    interface IScanResultCallback {
        fun onCallback(result: Barcode)
    }

}