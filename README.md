# Gesture Player — управление видеоплеером жестами руки (Android, Kotlin)

Приложение берёт видео с камеры телефона, находит руку, определяет жест и управляет плеером.

## Жесты

| Жест | Действие |
|------|----------|
| ✋ Открытая ладонь | Воспроизведение |
| ✊ Кулак | Пауза |
| 👍 Большой палец вверх | Громче (повторяется, пока держишь жест) |
| 👎 Большой палец вниз | Тише |
| ☝️ Указательный палец | Перемотка на 10 с вперёд |
| ✌️ Victory | Перемотка на 10 с назад |

## Технологии

- **Kotlin**, minSdk 26, targetSdk 34
- **CameraX** — превью и поток кадров (ImageAnalysis)
- **MediaPipe Tasks Vision (HandLandmarker)** — 21 ключевая точка руки
- **Media3 ExoPlayer** — воспроизведение видео
- ViewBinding, Material Components

## Как запустить

1. Открой папку проекта в **Android Studio** (Koala или новее) и дождись Gradle Sync.
2. Модель `hand_landmarker.task` скачивается автоматически при сборке
   в `app/src/main/assets/`. Если интернета при сборке нет, скачай вручную:
   <https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task>
   и положи файл в `app/src/main/assets/`.
3. Подключи телефон (отладка по USB) или эмулятор с камерой и нажми **Run**.
4. Разреши доступ к камере, нажми **Демо** (потоковое видео) или **Выбрать видео** (файлы с телефона, можно несколько).
5. Покажи руку в камеру на расстоянии 30–60 см при хорошем освещении.

## Как это работает

```
Камера (CameraX) → ImageProxy → Bitmap (поворот + зеркало)
   → HandLandmarker (21 точка) → GestureClassifier (геометрия пальцев)
   → GestureStabilizer (5 кадров подряд) → PlayerGestureController → ExoPlayer
```

- **GestureClassifier** — палец считается выпрямленным, если кончик дальше от запястья,
  чем средний сустав (PIP). Это не зависит от поворота руки и расстояния до камеры.
  Большой палец проверяется отдельно (вынос относительно размера ладони), направление вверх/вниз
  определяется по вертикальной составляющей вектора MCP → TIP.
- **GestureStabilizer** — жест принимается, только если держится 5 кадров подряд (убирает дребезг).
- **PlayerGestureController** — play/pause срабатывают один раз при смене жеста,
  громкость и перемотка повторяются с интервалом (500 мс и 900 мс).

## Что можно настроить

- Пороги распознавания: константы в `GestureClassifier.kt` (`FINGER_RATIO`, `THUMB_RATIO`, `THUMB_VERTICAL`).
- Устойчивость жеста: `framesRequired` в `MainActivity.kt`.
- Шаг перемотки и интервалы: `PlayerGestureController.kt`.

## Структура

```
app/src/main/java/com/example/gestureplayer/
├── MainActivity.kt                  — экран, камера, плеер
├── gesture/
│   ├── Gesture.kt                   — список жестов
│   ├── GestureClassifier.kt         — точки руки → жест
│   ├── GestureStabilizer.kt         — защита от дребезга
│   └── HandGestureAnalyzer.kt       — CameraX + MediaPipe
├── player/PlayerGestureController.kt — жест → команда плееру
└── ui/OverlayView.kt                — скелет руки поверх превью
```

## Презентация

В папке [`presentation/`](presentation/) лежит презентация проекта: готовый PDF
([`Gesture-Player.pdf`](presentation/Gesture-Player.pdf)) и исходники слайдов (HTML и CSS).
