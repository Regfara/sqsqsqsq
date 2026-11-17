package com.example.gestureplayer.player

import android.os.SystemClock
import androidx.media3.common.Player
import com.example.gestureplayer.gesture.Gesture
import kotlin.math.roundToInt

/**
 * Превращает устойчивый жест в команду плееру.
 *
 *  ✋ ладонь        — играть
 *  ✊ кулак         — пауза
 *  👍 палец вверх   — громче (повторяется, пока жест удерживается)
 *  👎 палец вниз    — тише
 *  ☝️ указательный  — перемотка на 10 секунд вперёд
 *  ✌️ victory       — перемотка на 10 секунд назад
 */
class PlayerGestureController(
    private val player: Player,
    private val onAction: (String) -> Unit
) {
    private var lastGesture = Gesture.NONE
    private val lastRepeat = HashMap<Gesture, Long>()

    fun onGesture(gesture: Gesture) {
        val changed = gesture != lastGesture
        lastGesture = gesture

        when (gesture) {
            Gesture.OPEN_PALM -> if (changed) {
                if (!player.isPlaying) {
                    player.play()
                    onAction("▶ Воспроизведение")
                }
            }
            Gesture.FIST -> if (changed) {
                if (player.isPlaying) {
                    player.pause()
                    onAction("⏸ Пауза")
                }
            }
            Gesture.THUMB_UP -> repeating(gesture, 500) { changeVolume(+0.1f) }
            Gesture.THUMB_DOWN -> repeating(gesture, 500) { changeVolume(-0.1f) }
            Gesture.POINTING -> repeating(gesture, 900) { seekBy(+SEEK_MS) }
            Gesture.VICTORY -> repeating(gesture, 900) { seekBy(-SEEK_MS) }
            Gesture.NONE -> Unit
        }
    }

    private fun repeating(gesture: Gesture, intervalMs: Long, action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        val last = lastRepeat[gesture] ?: 0L
        if (now - last >= intervalMs) {
            lastRepeat[gesture] = now
            action()
        }
    }

    private fun changeVolume(delta: Float) {
        val volume = (player.volume + delta).coerceIn(0f, 1f)
        player.volume = volume
        val icon = if (delta > 0) "🔊" else "🔉"
        onAction("$icon Громкость ${(volume * 100).roundToInt()}%")
    }

    private fun seekBy(deltaMs: Long) {
        val duration = player.duration
        var target = player.currentPosition + deltaMs
        target = if (duration > 0) target.coerceIn(0, duration) else target.coerceAtLeast(0)
        player.seekTo(target)
        onAction(if (deltaMs > 0) "⏩ +10 с" else "⏪ −10 с")
    }

    companion object {
        private const val SEEK_MS = 10_000L
    }
}
