package com.fieldrecord.presentation.map

import com.fieldrecord.data.local.entity.TrackPointEntity
import com.fieldrecord.data.local.entity.TrackSessionEntity

data class TrackSnapshot(
    val session: TrackSessionEntity,
    val points: List<TrackPointEntity>
)
