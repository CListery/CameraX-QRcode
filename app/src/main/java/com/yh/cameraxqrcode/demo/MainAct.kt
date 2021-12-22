package com.yh.cameraxqrcode.demo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.yh.cameraxqrcode.demo.databinding.ActMainBinding
import com.yh.cxqr.QRScannerView
import com.yh.cxqr.model.Barcode

class MainAct : AppCompatActivity() {

    private lateinit var binding: ActMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSwitchScanner.setOnClickListener {
            if (binding.scanner.isScanning) {
                binding.scanner.stopScan()
            } else {
                binding.scanner.beginScan()
            }
        }

        binding.btnSwitchFlash.setOnClickListener {
            binding.scanner.switchFlash()
        }

        binding.scanner.resultCallback = object : QRScannerView.IScanResultCallback {
            override fun onCallback(result: Barcode) {
                Toast.makeText(this@MainAct, "${result.key} , ${result.value}", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        binding.scanner.postDelayed({
            hideSystemUI()
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.scanner).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private companion object {
        private const val IMMERSIVE_FLAG_TIMEOUT = 500L
    }

}