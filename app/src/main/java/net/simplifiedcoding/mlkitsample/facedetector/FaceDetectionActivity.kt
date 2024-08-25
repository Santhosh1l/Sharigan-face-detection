package net.simplifiedcoding.mlkitsample.facedetector

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import net.simplifiedcoding.mlkitsample.CameraXViewModel
import net.simplifiedcoding.mlkitsample.databinding.ActivityFaceDetectionBinding
import java.util.concurrent.Executors

@OptIn(androidx.camera.core.ExperimentalGetImage::class) // Opt-in annotation for experimental API
class FaceDetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaceDetectionBinding
    private lateinit var cameraSelector: CameraSelector
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis

    private val cameraXViewModel by viewModels<CameraXViewModel>()

    private val KNOWN_WIDTH = 160 // Average width of a face in mm
    private val FOCAL_LENGTH = 600 // Focal length of the camera
    private val DANGER_DISTANCE = 400 // Distance in mm to warn the user

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
        cameraXViewModel.processCameraProvider.observe(this) { provider ->
            processCameraProvider = provider
            bindCameraPreview()
            bindInputAnalyser()
        }
    }

    private fun bindCameraPreview() {
        cameraPreview = Preview.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()
        cameraPreview.setSurfaceProvider(binding.previewView.surfaceProvider)
        try {
            processCameraProvider.bindToLifecycle(this, cameraSelector, cameraPreview)
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "Error binding camera preview")
        }
    }

    private fun bindInputAnalyser() {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .build()
        )
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        val cameraExecutor = Executors.newSingleThreadExecutor()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(detector, imageProxy)
        }

        try {
            processCameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "Error binding image analysis")
        }
    }

    @OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImageProxy(detector: FaceDetector, imageProxy: ImageProxy) {
        val inputImage = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                binding.graphicOverlay.clear()
                faces.forEach { face ->
                    val faceBox = FaceBox(binding.graphicOverlay, face, imageProxy.image!!.cropRect)
                    binding.graphicOverlay.add(faceBox)

                    val faceWidth = face.boundingBox.width()
                    val distance = calculateDistanceToCamera(KNOWN_WIDTH, FOCAL_LENGTH, faceWidth)

                    if (distance < DANGER_DISTANCE) {
                        showWarning("Go back! Your face is too close.")
                    } else {
                        showWarning("Safe distance.")
                    }
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun calculateDistanceToCamera(knownWidth: Int, focalLength: Int, perWidth: Int): Double {
        return (knownWidth * focalLength) / perWidth.toDouble()
    }

    private fun showWarning(message: String) {
        binding.warningTextView.text = message
    }

    companion object {
        private val TAG = FaceDetectionActivity::class.simpleName
        fun startActivity(context: Context) {
            Intent(context, FaceDetectionActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }
}
