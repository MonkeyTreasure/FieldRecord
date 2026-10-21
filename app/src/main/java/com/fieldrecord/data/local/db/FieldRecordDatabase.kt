package com.fieldrecord.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fieldrecord.data.local.dao.FieldPhotoDao
import com.fieldrecord.data.local.dao.TrackDao
import com.fieldrecord.data.local.entity.FieldPhotoEntity
import com.fieldrecord.data.local.entity.TrackPointEntity
import com.fieldrecord.data.local.entity.TrackSessionEntity

@Database(
    entities = [FieldPhotoEntity::class, TrackSessionEntity::class, TrackPointEntity::class],
    version = 2,
    exportSchema = false
)
abstract class FieldRecordDatabase : RoomDatabase() {
    abstract fun fieldPhotoDao(): FieldPhotoDao
    abstract fun trackDao(): TrackDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS track_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startedAt INTEGER NOT NULL,
                        endedAt INTEGER,
                        status TEXT NOT NULL,
                        pausedDurationMillis INTEGER NOT NULL,
                        pauseStartedAt INTEGER,
                        segmentIndex INTEGER NOT NULL,
                        distanceMeters REAL NOT NULL
                    )""".trimIndent()
                )
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS track_points (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        segmentIndex INTEGER NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        accuracyMeters REAL NOT NULL,
                        speedMetersPerSecond REAL NOT NULL,
                        FOREIGN KEY(sessionId) REFERENCES track_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )""".trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_track_points_sessionId_timestamp ON track_points(sessionId, timestamp)")
            }
        }

        @Volatile private var INSTANCE: FieldRecordDatabase? = null

        fun getDatabase(context: Context): FieldRecordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FieldRecordDatabase::class.java,
                    "field_record_db"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}