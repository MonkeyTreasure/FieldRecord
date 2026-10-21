package com.fieldrecord.di

import android.content.Context
import androidx.room.Room
import com.fieldrecord.data.local.db.FieldRecordDatabase
import com.fieldrecord.data.local.dao.FieldPhotoDao
import com.fieldrecord.data.local.dao.TrackDao
import com.fieldrecord.data.repository.FieldPhotoRepositoryImpl
import com.fieldrecord.domain.repository.FieldPhotoRepository
import com.fieldrecord.presentation.camera.CameraViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FieldRecordDatabase {
        return FieldRecordDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideDao(database: FieldRecordDatabase): FieldPhotoDao {
        return database.fieldPhotoDao()
    }

    @Provides
    fun provideTrackDao(database: FieldRecordDatabase): TrackDao {
        return database.trackDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideFieldPhotoRepository(impl: FieldPhotoRepositoryImpl): FieldPhotoRepository {
        return impl
    }
}
