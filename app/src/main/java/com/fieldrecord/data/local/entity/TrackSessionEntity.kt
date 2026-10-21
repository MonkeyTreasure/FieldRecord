package com.fieldrecord.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_sessions")
data class TrackSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
    val status: String = STATUS_RECORDING,
    val pausedDurationMillis: Long = 0,
    val pauseStartedAt: Long? = null,
    val segmentIndex: Int = 0,
    val distanceMeters: Double = 0.0
) {
    companion object {
        const val STATUS_RECORDING = "RECORDING"
        const val STATUS_PAUSED = "PAUSED"
        const val STATUS_COMPLETED = "COMPLETED"
    }
}
