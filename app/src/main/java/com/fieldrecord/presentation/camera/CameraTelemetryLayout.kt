package com.fieldrecord.presentation.camera

internal data class CameraTelemetryOrientation(
    val rotationDegrees: Int,
    val pivotX: Float,
    val pivotY: Float
)

internal fun cameraTelemetryOrientation(surfaceRotation: Int): CameraTelemetryOrientation {
    val degrees = (((surfaceRotation % 4) + 4) % 4) * 90
    val pivot = when (degrees) {
        90 -> 1f to 1f
        180 -> 1f to 0f
        270 -> 0f to 0f
        else -> 0f to 1f
    }
    return CameraTelemetryOrientation(degrees, pivot.first, pivot.second)
}

internal fun telemetryUsesScreenBottomAnchor(isLandscape: Boolean): Boolean = isLandscape
