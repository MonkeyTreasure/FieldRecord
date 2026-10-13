package com.fieldrecord.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.domain.repository.FieldPhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: FieldPhotoRepository
) : ViewModel() {
    val photos: StateFlow<List<FieldPhoto>> = repository.getAllPhotos().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    suspend fun updatePhotoNote(photoId: Long, note: String?) {
        repository.updatePhotoNote(photoId, note)
    }
}
