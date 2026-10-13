package com.fieldrecord.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fieldrecord.domain.model.FieldPhoto
import java.io.File

@Entity(tableName = "field_photos")
data class FieldPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val latitude: Double,
    val longitude: Double,
    val bearing: Float,
    val timestamp: Long,
    val note: String? = null
)

fun FieldPhotoEntity.toDomain() = FieldPhoto(id, filePath, fileName, latitude, longitude, bearing, timestamp, note)
fun FieldPhoto.toEntity() = FieldPhotoEntity(id, filePath, fileName, latitude, longitude, bearing, timestamp, note)
