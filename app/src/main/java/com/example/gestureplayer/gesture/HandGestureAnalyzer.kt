package com.example.gestureplayer.gesture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker

/** Результат анализа одного кадра. */
data class HandFrame(
    /** Точки руки в нормированных координатах (0..1) или пусто, если руки нет. */
    val points: List<Pair<Float, Float>>,
    val imageWidth: Int,
    val imageHeight: Int,
    val gesture: Gesture
)

/**
 * Берёт кадры с камеры (CameraX), находит руку через MediaPipe HandLandmarker
 * и определяет жест. Результат отдаётся в главный поток.
 */
class HandGestureAnalyzer(
    context: Context,
    @Volatile var isFrontCamera: Boolean,
    private val onFrame: (HandFrame) -> Unit
) : ImageAnalysis.Analyzer, AutoCloseable {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val landmarker: HandLandmarker = HandLandmarker.createFromOptions(
        context,
        HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder().setModelAssetPath(MODEL_ASSET).build()
            )
            .setRunningMode(RunningMode.IMAGE)
            .setNumHands(1)
            .setMinHandDetectionConfidence(0.5f)
            .setMinHandPresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build()
    )

    override fun analyze(image: ImageProxy) {
        try {
            val bitmap = prepareBitmap(image)
            val result = landmarker.detect(BitmapImageBuilder(bitmap).build())

            val hand = result.landmarks().firstOrNull()
            val frame = if (hand == null) {
                HandFrame(emptyList(), bitmap.width, bitmap.height, Gesture.NONE)
            } else {
                HandFrame(
                    points = hand.map { it.x() to it.y() },
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height,
                    gesture = GestureClassifier.classify(hand, bitmap.width, bitmap.height)
                )
            }
            mainHandler.post { onFrame(frame) }
        } catch (e: Exception) {
            // Один неудачный кадр не должен ронять приложение
        } finally {
            image.close()
        }
    }

    /** Поворачивает кадр «вертикально» и зеркалит его для фронтальной камеры. */
    private fun prepareBitmap(image: ImageProxy): Bitmap {
        val source = image.toBitmap()
        val matrix = Matrix().apply {
            postRotate(image.imageInfo.rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f, source.width / 2f, source.height / 2f)
            }
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    override fun close() {
        landmarker.close()
    }

    companion object {
        private const val MODEL_ASSET = "hand_landmarker.task"
    }
}
