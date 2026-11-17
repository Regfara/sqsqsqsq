plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.gestureplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gestureplayer"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    // Модель MediaPipe нельзя сжимать, иначе она не загрузится из assets
    androidResources {
        noCompress += listOf("task", "tflite")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // Камера
    val cameraX = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")

    // Видеоплеер
    val media3 = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-ui:$media3")

    // Распознавание рук (21 ключевая точка)
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
}

// ---------------------------------------------------------------------------
// Автоматическая загрузка модели hand_landmarker.task перед сборкой,
// если её ещё нет в app/src/main/assets
// ---------------------------------------------------------------------------
val modelFile = layout.projectDirectory.file("src/main/assets/hand_landmarker.task").asFile

val downloadHandModel by tasks.registering {
    onlyIf { !modelFile.exists() }
    doLast {
        modelFile.parentFile.mkdirs()
        val url = "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task"
        uri(url).toURL().openStream().use { input ->
            modelFile.outputStream().use { output -> input.copyTo(output) }
        }
        println("Модель загружена: ${modelFile.absolutePath}")
    }
}

tasks.named("preBuild") {
    dependsOn(downloadHandModel)
}
