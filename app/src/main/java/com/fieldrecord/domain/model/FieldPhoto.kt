package com.fieldrecord.domain.model

import java.io.File

data class FieldPhoto(
    val id: Long,
    val filePath: String,
    val fileName: String,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float,
    val timestamp: Long,
    val note: String? = null
) {
    fun toFile(): File = File(filePath)
}