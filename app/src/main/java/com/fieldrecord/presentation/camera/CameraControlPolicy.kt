package com.fieldrecord.presentation.camera

enum class FlashSetting { OFF, AUTO, ON }

fun FlashSetting.toImageCaptureFlashMode(): Int = when (this) {
    FlashSetting.OFF -> androidx.camera.core.ImageCapture.FLASH_MODE_OFF
    FlashSetting.AUTO -> androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
    FlashSetting.ON -> androidx.camera.core.ImageCapture.FLASH_MODE_ON
}

fun nextFlashSetting(setting: FlashSetting): FlashSetting = when (setting) {
    FlashSetting.OFF -> FlashSetting.AUTO
    FlashSetting.AUTO -> FlashSetting.ON
    FlashSetting.ON -> FlashSetting.OFF
}

fun clampZoomRatio(requested: Float, minRatio: Float, maxRatio: Float): Float {
    if (!minRatio.isFinite() || !maxRatio.isFinite() || minRatio <= 0f || maxRatio < minRatio) return 1f
    return requested.coerceIn(minRatio, maxRatio)
}

fun supportedZoomPresets(minRatio: Float, maxRatio: Float): List<Float> {
    if (!minRatio.isFinite() || !maxRatio.isFinite() || minRatio <= 0f || maxRatio < minRatio) return emptyList()
    return listOf(0.5f, 1f, 2f).filter { it >= minRatio && it <= maxRatio }
}
