package net.simplifiedcoding.mlkitsample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import net.simplifiedcoding.mlkitsample.databinding.ActivityMainBinding
import net.simplifiedcoding.mlkitsample.facedetector.FaceDetectionActivity

class MainActivity : AppCompatActivity() {

    private val cameraPermission = android.Manifest.permission.CAMERA
    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startFaceDetection()
            }
        }

    private val faceDetectionResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val distance = result.data?.getDoubleExtra("DISTANCE", -1.0) ?: -1.0
                displayDistance(distance)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonFaceDetect.setOnClickListener {
            requestCameraAndStart()
        }
    }

    private fun requestCameraAndStart() {
        if (isPermissionGranted(cameraPermission)) {
            startFaceDetection()
        } else {
            requestCameraPermission()
        }
    }

    private fun startFaceDetection() {
        val intent = Intent(this, FaceDetectionActivity::class.java)
        faceDetectionResultLauncher.launch(intent)
    }

    private fun requestCameraPermission() {
        when {
            shouldShowRequestPermissionRationale(cameraPermission) -> {
                cameraPermissionRequest(
                    positive = { openPermissionSetting() }
                )
            }
            else -> {
                requestPermissionLauncher.launch(cameraPermission)
            }
        }
    }

    private fun displayDistance(distance: Double) {
        val message = if (distance >= 0) {
            "Distance: ${distance.toInt()} mm"
        } else {
            "Unable to determine distance."
        }
        binding.distanceTextView.text = message
    }
}
