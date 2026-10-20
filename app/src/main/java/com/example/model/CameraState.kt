package com.example.model

import android.net.Uri

enum class CameraMode {
    PHOTO,
    VIDEO,
    NIGHT_AI,
    PRO_100X
}

enum class FlashMode {
    OFF,
    ON,
    AUTO
}

enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    ARABIC
}

enum class LiveFilter {
    ORIGINAL,
    AI_BOOST,
    CYBER_HUD,
    NIGHT_OWL,
    MONOCHROME
}

data class CapturedMedia(
    val id: String,
    val uri: Uri,
    val filePath: String,
    val isVideo: Boolean,
    val timestamp: Long,
    val zoomFactor: Float,
    val mode: CameraMode,
    val isAiEnhanced: Boolean,
    val originalFilePath: String? = null,
    val durationSeconds: Long = 0
)

data class CameraSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val aiClarityBoostEnabled: Boolean = true,
    val noiseReductionEnabled: Boolean = true,
    val smartSharpeningEnabled: Boolean = true,
    val hdrFusionEnabled: Boolean = true,
    val superResolutionEnabled: Boolean = true,
    val showHudGrid: Boolean = true,
    val flashMode: FlashMode = FlashMode.AUTO,
    val ultraQualityMode: Boolean = true // true = deep 100X neural fusion, false = fast
)
