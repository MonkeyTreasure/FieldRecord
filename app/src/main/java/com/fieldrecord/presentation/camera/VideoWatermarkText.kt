package com.fieldrecord.presentation.camera

import java.util.Locale

internal data class VideoWatermarkLines(
    val unitId: String,
    val date: String,
    val location: String
)

internal fun formatRecordingDuration(elapsedSeconds: Int): String {
    val seconds = elapsedSeconds.coerceAtLeast(0)
    return if (seconds >= 3_600) {
        "REC %02d:%02d:%02d".format(Locale.US, seconds / 3_600, (seconds / 60) % 60, seconds % 60)
    } else {
        "REC %02d:%02d".format(Locale.US, seconds / 60, seconds % 60)
    }
}

internal fun recordingTimerLabel(
    isStartingVideo: Boolean,
    isRecording: Boolean,
    elapsedSeconds: Int
): String? = when {
    isStartingVideo -> formatRecordingDuration(0)
    isRecording -> formatRecordingDuration(elapsedSeconds)
    else -> null
}

internal fun videoModeRecordMockupLabel(
    isVideoMode: Boolean,
    isStartingVideo: Boolean,
    isRecording: Boolean,
    elapsedSeconds: Int
): String? = if (!isVideoMode) null else
    recordingTimerLabel(isStartingVideo, isRecording, elapsedSeconds) ?: formatRecordingDuration(0)

internal fun shouldRenderVideoWatermark(isStartingVideo: Boolean, isRecording: Boolean): Boolean =
    isStartingVideo || isRecording

internal fun formatVideoWatermarkLines(
    unitId: String,
    dateText: String,
    latitude: Double?,
    longitude: Double?,
    bearingDegrees: Float
): VideoWatermarkLines {
    val hasCoordinates = latitude != null && longitude != null &&
        latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
        !(latitude == 0.0 && longitude == 0.0)
    val hasBearing = bearingDegrees.isFinite() && bearingDegrees in 0f..360f
    val location = if (!hasCoordinates) {
        "GPS unavailable"
    } else {
        val coordinates = String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
        if (hasBearing) {
            val cardinals = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            val cardinal = cardinals[((bearingDegrees + 22.5f) / 45f).toInt() % cardinals.size]
            "$coordinates ($cardinal - ${bearingDegrees.toInt()}°)"
        } else {
            coordinates
        }
    }
    return VideoWatermarkLines(
        unitId = unitId.ifBlank { "SBYE" },
        date = dateText,
        location = location
    )
}
