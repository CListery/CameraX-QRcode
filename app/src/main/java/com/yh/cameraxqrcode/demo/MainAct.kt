package com.yh.cameraxqrcode.demo

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.yh.cameraxqrcode.demo.databinding.ActMainBinding
import com.yh.cxqr.QRScannerView
import com.yh.cxqr.model.Barcode
import com.yh.sarl.launcher.ContractType
import com.yh.sarl.launcher.SimpleLauncher
import com.yh.sarl.launcher.SimpleLauncher.Companion.simpleLauncher
import com.yh.sarl.onFailure
import com.yh.sarl.onSuccess
import kotlin.time.ExperimentalTime

class MainAct : AppCompatActivity() {

    private lateinit var binding: ActMainBinding

    private val permissionsLauncher: SimpleLauncher<Array<String>, Map<String, Boolean>> =
        simpleLauncher(ContractType.RequestMultiplePermissions, this)
    private val openPhotoLauncher: SimpleLauncher<Array<String>, Uri> =
        simpleLauncher(ContractType.OpenDocument, this)

    @ExperimentalTime
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionsLauncher
            .input(
                arrayOf(
                    Manifest.permission.CAMERA
                )
            )
            .checker { value -> value?.all { it.value } ?: false }
            .launch()
            .onSuccess {
                binding = ActMainBinding.inflate(layoutInflater)
                setContentView(binding.root)

                initView()
            }
            .onFailure {
                finish()
            }
    }

    @ExperimentalTime
    private fun initView() {
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

        binding.btnXc.setOnClickListener {
            permissionsLauncher
                .input(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
                .checker { value -> value?.all { it.value } ?: false }
                .next(openPhotoLauncher)
                .input(arrayOf("image/*"))
                .checker { null != it }
                .launch()
                .onSuccess {
                    binding.scanner.stopScan()
                    binding.scanner.decodeImageUriWithTimed(it,
                        success = { result ->
                            Log.d("MainAct", "decodeImageUri: $result")
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainAct,
                                    "${result.key} , ${result.value}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        {
                            Log.d("MainAct", "decodeImageUri: fail")
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainAct,
                                    "解码失败",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }
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
        try {// Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
            // be trying to set app to immersive mode before it's ready and the flags do not stick
            binding.scanner.postDelayed({
                hideSystemUI()
            }, IMMERSIVE_FLAG_TIMEOUT)
        } catch (e: Exception) {
        }
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