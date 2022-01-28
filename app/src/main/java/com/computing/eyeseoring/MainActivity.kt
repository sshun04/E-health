package com.computing.eyeseoring

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.computing.eyeseoring.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), SensorEventListener2 {

    private var sensorManger: SensorManager? = null
    private lateinit var binding: ActivityMainBinding

    private val detector: FaceDetector

    init {
        val options = FaceDetectorOptions.Builder()
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()

        detector = FaceDetection.getClient(options)
    }

    private lateinit var cameraExecutor: ExecutorService

    companion object {
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

        private const val TAG = "EyeCensoring"
        private const val REQUEST_CODE_PERMISSIONS = 10
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        sensorManger = getSystemService(SENSOR_SERVICE) as? SensorManager


        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun detectFaces(image: InputImage) {
        detector.process(image)
            .addOnSuccessListener { faces ->
                for (face in faces) {
                    if (face.rightEyeOpenProbability != null && face.leftEyeOpenProbability != null) {
                        val rightEyeOpenProb = face.rightEyeOpenProbability
                        val leftEyeOpenProb = face.leftEyeOpenProbability

                        binding.eyeOpenProbText.text =
                            "left : $leftEyeOpenProb , right: $rightEyeOpenProb"
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "Face detection failed")
            }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor, PreviewAnalyzer(
                            object : PreviewListener {
                                @SuppressLint("UnsafeOptInUsageError")
                                override fun onImageInput(image: ImageProxy) {
                                    Log.d(TAG, "Average luminosity:")

                                    BitmapUtils.getBitmap(image)?.let { bitmap ->
                                        val input = InputImage.fromBitmap(bitmap, 0)
                                        detectFaces(input)
                                    }

                                    image.close()
                                }
                            })
                    )
                }

            // Select front camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onResume() {
        super.onResume()
        sensorManger?.apply {
            getSensorList(Sensor.TYPE_LIGHT).let { sensorsList ->
                if (sensorsList.isNotEmpty()) {
                    val sensor = sensorsList[0]
                    this.registerListener(this@MainActivity, sensor, SensorManager.SENSOR_DELAY_UI)
                }
            }
        }
    }

    override fun onFlushCompleted(p0: Sensor?) { /*Do nothing*/
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) { /*Do nothing*/
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (event.sensor.type == Sensor.TYPE_LIGHT) {
                with(binding) {
                    val brightness = event.values[0]
                    brightnessValueText.text = "照度 :" + brightness.toString()
                    confirmationText.text = when {
                        brightness < 150 -> "暗いよ？"
                        else -> "良い感じ"
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        sensorManger?.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}