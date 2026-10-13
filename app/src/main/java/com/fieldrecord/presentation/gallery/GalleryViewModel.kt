package com.fieldrecord.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.domain.export.PhotoNotesCsvExporter
import com.fieldrecord.domain.repository.FieldPhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: FieldPhotoRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar> = _currentMonth

    val pager = _currentMonth.flatMapLatest { calendar ->
        Pager(PagingConfig(pageSize = 20)) {
            val startTime = calendar.clone() as Calendar
            startTime.set(Calendar.DAY_OF_MONTH, 1)
            startTime.set(Calendar.HOUR_OF_DAY, 0)
            startTime.set(Calendar.MINUTE, 0)

            val endTime = calendar.clone() as Calendar
            endTime.set(Calendar.DAY_OF_MONTH, endTime.getActualMaximum(Calendar.DAY_OF_MONTH))
            endTime.set(Calendar.HOUR_OF_DAY, 23)
            endTime.set(Calendar.MINUTE, 59)

            FieldPhotoPagingSource(repository, startTime.timeInMillis, endTime.timeInMillis)
        }.flow
    }.cachedIn(viewModelScope)

    // Auto-sync photos from storage folder to database
    fun syncPhotosFromStorage(context: Context) {
        viewModelScope.launch {
            val storageDir = com.fieldrecord.presentation.camera.PhotoFileManager.getStorageDir()
            val files = storageDir.listFiles()?.filter { it.isFile && it.extension.equals("jpg", true) }?.toList() ?: emptyList()
            if (files.isNotEmpty()) {
                repository.syncPhotos(files, context)
            }
        }
    }

    suspend fun deleteSelected(photos: List<FieldPhoto>) {
        photos.forEach { photo ->
            repository.deletePhoto(photo)
            java.io.File(photo.filePath).delete()
        }
    }

    suspend fun updatePhotoNote(photoId: Long, note: String?) {
        repository.updatePhotoNote(photoId, note)
    }

    suspend fun exportPhotoNotesCsv(): String? {
        val notedPhotos = repository.getPhotosWithNotes()
        return notedPhotos.takeIf { it.isNotEmpty() }?.let(PhotoNotesCsvExporter::export)
    }

    fun nextMonth() {
        val next = _currentMonth.value.clone() as Calendar
        next.add(Calendar.MONTH, 1)
        _currentMonth.value = next
    }

    fun previousMonth() {
        val prev = _currentMonth.value.clone() as Calendar
        prev.add(Calendar.MONTH, -1)
        _currentMonth.value = prev
    }

    fun getCurrentMonthLabel(): String {
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(_currentMonth.value.time)
    }
}
