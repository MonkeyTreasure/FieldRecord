package com.fieldrecord.domain.model

data class FieldVideo(
    val id: Long,
    val filePath: String,
    val fileName: String,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float,
    val timestamp: Long,
    val durationMs: Long,
    val note: String?
)
