package com.fieldrecord.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fieldrecord.data.local.entity.TrackPointEntity
import com.fieldrecord.data.local.entity.TrackSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Insert
    suspend fun insertSession(session: TrackSessionEntity): Long

    @Update
    suspend fun updateSession(session: TrackSessionEntity)

    @Insert
    suspend fun insertPoint(point: TrackPointEntity)

    @Query("SELECT * FROM track_sessions ORDER BY startedAt DESC, id DESC LIMIT 1")
    fun observeLatestSession(): Flow<TrackSessionEntity?>

    @Query("SELECT * FROM track_sessions WHERE status IN ('RECORDING', 'PAUSED') ORDER BY startedAt DESC, id DESC LIMIT 1")
    suspend fun getActiveSession(): TrackSessionEntity?

    @Query("SELECT * FROM track_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: Long): TrackSessionEntity?

    @Query("UPDATE track_sessions SET distanceMeters = distanceMeters + :distanceMeters WHERE id = :sessionId")
    suspend fun addDistance(sessionId: Long, distanceMeters: Double)

    @Transaction
    suspend fun appendPoint(point: TrackPointEntity, distanceMeters: Double) {
        insertPoint(point)
        if (distanceMeters > 0.0) addDistance(point.sessionId, distanceMeters)
    }

    @Query("SELECT * FROM track_points WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC")
    fun observePoints(sessionId: Long): Flow<List<TrackPointEntity>>

    @Query("SELECT * FROM track_points WHERE sessionId = :sessionId AND segmentIndex = :segmentIndex ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLastPoint(sessionId: Long, segmentIndex: Int): TrackPointEntity?
}
