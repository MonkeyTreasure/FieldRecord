package com.fieldrecord.domain.repository

import com.fieldrecord.domain.model.FieldPhoto
import kotlinx.coroutines.flow.Flow
import java.io.File
import android.content.Context

interface FieldPhotoRepository {
    fun getAllPhotos(): Flow<List<FieldPhoto>>
    suspend fun getPhotosByDateRange(start: Long, end: Long): List<FieldPhoto>
    suspend fun getPhotosWithNotes(): List<FieldPhoto>
    suspend fun syncPhotos(files: List<File>, context: Context)
    suspend fun updatePhotoNote(photoId: Long, note: String?)
    suspend fun insertPhoto(photo: FieldPhoto): Long
    suspend fun deletePhoto(photo: FieldPhoto)
}
