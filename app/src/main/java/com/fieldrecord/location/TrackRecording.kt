package com.fieldrecord.location

import com.fieldrecord.data.local.entity.TrackPointEntity
import com.fieldrecord.data.local.entity.TrackSessionEntity
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class TrackFix(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float
)

internal const val MAX_TRACK_ACCURACY_METERS = 35f
internal const val TRACK_MIN_DISTANCE_METERS = 3.0

internal fun shouldRecordTrackFix(previous: TrackPointEntity?, fix: TrackFix): Boolean {
    if (!fix.latitude.isFinite() || !fix.longitude.isFinite()) return false
    if (fix.latitude !in -90.0..90.0 || fix.longitude !in -180.0..180.0) return false
    if (fix.latitude == 0.0 && fix.longitude == 0.0) return false
    if (!fix.accuracyMeters.isFinite() || fix.accuracyMeters !in 0f..MAX_TRACK_ACCURACY_METERS) return false
    if (previous == null) return true
    if (fix.timestamp <= previous.timestamp) return false
    val averageAccuracy = (previous.accuracyMeters + fix.accuracyMeters) / 2.0
    val movementThreshold = maxOf(TRACK_MIN_DISTANCE_METERS, (averageAccuracy / 2.0).coerceAtMost(8.0))
    return trackDistanceMeters(previous.latitude, previous.longitude, fix.latitude, fix.longitude) >= movementThreshold
}

internal fun trackDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6_371_000.0
    val latitude1 = Math.toRadians(lat1)
    val latitude2 = Math.toRadians(lat2)
    val latitudeDelta = latitude2 - latitude1
    val longitudeDelta = Math.toRadians(lon2 - lon1)
    val a = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
        cos(latitude1) * cos(latitude2) * sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
    return earthRadius * 2 * atan2(sqrt(a.coerceIn(0.0, 1.0)), sqrt((1.0 - a).coerceIn(0.0, 1.0)))
}

internal fun elapsedTrackMillis(session: TrackSessionEntity, now: Long): Long {
    val endpoint = when (session.status) {
        TrackSessionEntity.STATUS_PAUSED -> session.pauseStartedAt ?: now
        TrackSessionEntity.STATUS_COMPLETED -> session.endedAt ?: now
        else -> now
    }
    return (endpoint - session.startedAt - session.pausedDurationMillis).coerceAtLeast(0L)
}
