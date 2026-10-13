package com.fieldrecord.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import com.fieldrecord.data.local.entity.FieldPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldPhotoDao {
    @Insert
    suspend fun insertPhoto(photo: FieldPhotoEntity): Long
    
    @Query("SELECT * FROM field_photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<FieldPhotoEntity>>
    
    @Query("SELECT * FROM field_photos")
    suspend fun getAllPhotosRaw(): List<FieldPhotoEntity>
    
    @Query("SELECT * FROM field_photos WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    suspend fun getPhotosByDateRange(start: Long, end: Long): List<FieldPhotoEntity>

    @Query("SELECT * FROM field_photos WHERE note IS NOT NULL AND TRIM(note) <> '' ORDER BY timestamp ASC")
    suspend fun getPhotosWithNotes(): List<FieldPhotoEntity>

    @Query("UPDATE field_photos SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String?)
    
    @Delete
    suspend fun deletePhoto(photo: FieldPhotoEntity)
    
    @Query("DELETE FROM field_photos WHERE id = :id")
    suspend fun deleteById(id: Long)
}
