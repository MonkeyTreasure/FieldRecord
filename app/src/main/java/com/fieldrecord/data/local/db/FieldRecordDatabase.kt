package com.fieldrecord.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fieldrecord.data.local.dao.FieldPhotoDao
import com.fieldrecord.data.local.entity.FieldPhotoEntity

@Database(entities = [FieldPhotoEntity::class], version = 1, exportSchema = false)
abstract class FieldRecordDatabase : RoomDatabase() {
    abstract fun fieldPhotoDao(): FieldPhotoDao
    
    companion object {
        @Volatile private var INSTANCE: FieldRecordDatabase? = null
        
        fun getDatabase(context: Context): FieldRecordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FieldRecordDatabase::class.java,
                    "field_record_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}