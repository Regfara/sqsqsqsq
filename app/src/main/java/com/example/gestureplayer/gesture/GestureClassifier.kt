package com.example.gestureplayer.gesture

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.hypot

/**
 * Классификатор жестов по 21 ключевой точке руки (MediaPipe Hands).
 *
 * Нумерация точек:
 *  0 — запястье
 *  1-4 — большой палец (CMC, MCP, IP, TIP)
 *  5-8 — указательный (MCP, PIP, DIP, TIP)
 *  9-12 — средний, 13-16 — безымянный, 17-20 — мизинец
 *
 * Палец считается выпрямленным, если кончик дальше от запястья, чем средний сустав (PIP).
 * Такой критерий не зависит от поворота руки и расстояния до камеры.
 */
object GestureClassifier {

    private const val WRIST = 0
    private const val THUMB_MCP = 2
    private const val THUMB_TIP = 4
    private const val INDEX_MCP = 5
    private const val MIDDLE_MCP = 9

    private const val FINGER_RATIO = 1.1f      // tip/pip относительно запястья
    private const val THUMB_RATIO = 0.55f      // вынос большого пальца относительно размера ладони
    private const val THUMB_VERTICAL = 0.6f    // доля вертикальной составляющей вектора большого пальца

    /**
     * @param imageWidth  ширина кадра в пикселях (нужна, т.к. x и y нормированы по-разному)
     * @param imageHeight высота кадра в пикселях
     */
    fun classify(lm: List<NormalizedLandmark>, imageWidth: Int, imageHeight: Int): Gesture {
        if (lm.size < 21) return Gesture.NONE

        val w = imageWidth.toFloat()
        val h = imageHeight.toFloat()

        val index = isFingerExtended(lm, 8, 6, w, h)
        val middle = isFingerExtended(lm, 12, 10, w, h)
        val ring = isFingerExtended(lm, 16, 14, w, h)
        val pinky = isFingerExtended(lm, 20, 18, w, h)
        val thumb = isThumbExtended(lm, w, h)

        val fourFingers = index && middle && ring && pinky
        val noFingers = !index && !middle && !ring && !pinky

        return when {
            fourFingers -> Gesture.OPEN_PALM
            index && middle && !ring && !pinky -> Gesture.VICTORY
            index && !middle && !ring && !pinky -> Gesture.POINTING
            noFingers && thumb -> thumbDirection(lm, w, h)
            noFingers -> Gesture.FIST
            else -> Gesture.NONE
        }
    }

    private fun dist(a: NormalizedLandmark, b: NormalizedLandmark, w: Float, h: Float): Float =
        hypot((a.x() - b.x()) * w, (a.y() - b.y()) * h)

    private fun isFingerExtended(
        lm: List<NormalizedLandmark>, tip: Int, pip: Int, w: Float, h: Float
    ): Boolean {
        val wrist = lm[WRIST]
        return dist(lm[tip], wrist, w, h) > dist(lm[pip], wrist, w, h) * FINGER_RATIO
    }

    private fun isThumbExtended(lm: List<NormalizedLandmark>, w: Float, h: Float): Boolean {
        val palmSize = dist(lm[WRIST], lm[MIDDLE_MCP], w, h)
        if (palmSize < 1f) return false
        return dist(lm[THUMB_TIP], lm[INDEX_MCP], w, h) / palmSize > THUMB_RATIO
    }

    /** Большой палец вверх / вниз / в сторону (тогда считаем кулаком). */
    private fun thumbDirection(lm: List<NormalizedLandmark>, w: Float, h: Float): Gesture {
        val dy = (lm[THUMB_TIP].y() - lm[THUMB_MCP].y()) * h   // ось Y направлена вниз
        val length = dist(lm[THUMB_TIP], lm[THUMB_MCP], w, h)
        if (length < 1f) return Gesture.FIST
        return when {
            dy < -THUMB_VERTICAL * length -> Gesture.THUMB_UP
            dy > THUMB_VERTICAL * length -> Gesture.THUMB_DOWN
            else -> Gesture.FIST
        }
    }
}
