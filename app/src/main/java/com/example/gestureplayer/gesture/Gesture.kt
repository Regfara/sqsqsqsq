package com.example.gestureplayer.gesture

/** Жесты, которые умеет распознавать приложение. */
enum class Gesture(val label: String) {
    NONE("—"),
    OPEN_PALM("Ладонь ✋"),
    FIST("Кулак ✊"),
    THUMB_UP("Палец вверх 👍"),
    THUMB_DOWN("Палец вниз 👎"),
    POINTING("Указательный ☝️"),
    VICTORY("Victory ✌️")
}
