package com.example.gestureplayer

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.gestureplayer.databinding.ActivityMainBinding
import com.example.gestureplayer.gesture.GestureStabilizer
import com.example.gestureplayer.gesture.HandFrame
import com.example.gestureplayer.gesture.HandGestureAnalyzer
import com.example.gestureplayer.player.PlayerGestureController
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var player: ExoPlayer
    private lateinit var controller: PlayerGestureController
    private lateinit var cameraExecutor: ExecutorService

    private val stabilizer = GestureStabilizer(framesRequired = 5)
    private var analyzer: HandGestureAnalyzer? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(this, R.string.camera_denied, Toast.LENGTH_LONG).show()
        }

    private val pickVideos =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) playUris(uris)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player

        controller = PlayerGestureController(player) { text ->
            binding.tvAction.text = text
        }

        binding.btnPick.setOnClickListener { pickVideos.launch(arrayOf("video/*")) }
        binding.btnDemo.setOnClickListener { playUris(listOf(Uri.parse(DEMO_URL))) }
        binding.btnSwitch.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
            startCamera()
        }

        if (hasCameraPermission()) startCamera()
        else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun playUris(uris: List<Uri>) {
        player.setMediaItems(uris.map { MediaItem.fromUri(it) })
        player.prepare()
        player.play()
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider
            val isFront = lensFacing == CameraSelector.LENS_FACING_FRONT

            val gestureAnalyzer = analyzer?.also { it.isFrontCamera = isFront }
                ?: HandGestureAnalyzer(this, isFront, ::onHandFrame).also { analyzer = it }

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(cameraExecutor, gestureAnalyzer) }

            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, selector, preview, analysis)
            } catch (e: Exception) {
                Toast.makeText(this, "Не удалось запустить камеру: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun onHandFrame(frame: HandFrame) {
        binding.overlayView.setHand(frame.points, frame.imageWidth, frame.imageHeight)

        val stable = stabilizer.update(frame.gesture)
        binding.tvGesture.text =
            if (frame.points.isEmpty()) getString(R.string.gesture_none)
            else "Жест: ${stable.label}"

        controller.onGesture(stable)
    }

    override fun onStop() {
        super.onStop()
        player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        cameraExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)
        analyzer?.close()
        player.release()
    }

    companion object {
        private const val DEMO_URL =
            "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
    }
}
