package com.example.gestureplayer.gesture

/**
 * Отсекает «дребезг»: жест считается устойчивым, только если он
 * держится [framesRequired] кадров подряд.
 */
class GestureStabilizer(private val framesRequired: Int = 5) {

    private var candidate = Gesture.NONE
    private var count = 0

    var stable: Gesture = Gesture.NONE
        private set

    fun update(raw: Gesture): Gesture {
        if (raw == candidate) {
            count++
        } else {
            candidate = raw
            count = 1
        }
        if (count >= framesRequired) stable = candidate
        return stable
    }

    fun reset() {
        candidate = Gesture.NONE
        count = 0
        stable = Gesture.NONE
    }
}
