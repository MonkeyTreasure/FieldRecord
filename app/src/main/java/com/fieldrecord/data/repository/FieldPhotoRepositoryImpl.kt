package com.fieldrecord.data.repository

import com.fieldrecord.data.local.dao.FieldPhotoDao
import com.fieldrecord.data.local.entity.toDomain
import com.fieldrecord.data.local.entity.toEntity
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.domain.repository.FieldPhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.content.Context
import androidx.exifinterface.media.ExifInterface
import java.io.File
import javax.inject.Inject

class FieldPhotoRepositoryImpl @Inject constructor(
    private val dao: FieldPhotoDao
) : FieldPhotoRepository {

    override fun getAllPhotos(): Flow<List<FieldPhoto>> {
        return dao.getAllPhotos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPhotosByDateRange(start: Long, end: Long): List<FieldPhoto> {
        return dao.getPhotosByDateRange(start, end).map { it.toDomain() }
    }

    override suspend fun getPhotosWithNotes(): List<FieldPhoto> {
        return dao.getPhotosWithNotes().map { it.toDomain() }
    }

    override suspend fun syncPhotos(files: List<File>, context: Context) {
        val existing = dao.getAllPhotosRaw()
        files.forEach { file ->
            if (existing.none { it.filePath == file.absolutePath }) {
                var lat = 0.0
                var lon = 0.0
                var bearing = -1f
                runCatching {
                    val exif = ExifInterface(file.absolutePath)
                    val latLon = exif.latLong
                    if (latLon != null) {
                        lat = latLon[0]
                        lon = latLon[1]
                    }
                    bearing = exif.getAttributeDouble(ExifInterface.TAG_GPS_IMG_DIRECTION, -1.0).toFloat()
                }
                val photo = FieldPhoto(
                    id = 0,
                    filePath = file.absolutePath,
                    fileName = file.name,
                    latitude = lat,
                    longitude = lon,
                    bearing = bearing,
                    timestamp = file.lastModified()
                )
                dao.insertPhoto(photo.toEntity())
            }
        }
    }

    override suspend fun updatePhotoNote(photoId: Long, note: String?) {
        dao.updateNote(photoId, note?.trim()?.takeIf { it.isNotEmpty() })
    }

    override suspend fun insertPhoto(photo: FieldPhoto): Long {
        return dao.insertPhoto(photo.toEntity())
    }

    override suspend fun findByMetadata(lat: Double, lon: Double, timestamp: Long): FieldPhoto? {
        return dao.findByMetadata(lat, lon, timestamp)?.toDomain()
    }

    override suspend fun deletePhoto(photo: FieldPhoto) {
        dao.deletePhoto(photo.toEntity())
    }
}
