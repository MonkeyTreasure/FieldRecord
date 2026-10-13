package com.fieldrecord.presentation.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldrecord.domain.repository.FieldPhotoRepository
import com.fieldrecord.domain.model.FieldPhoto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(private val repo: FieldPhotoRepository) : ViewModel() {
    private val _notedPhotos = MutableStateFlow<List<FieldPhoto>>(emptyList())
    val notedPhotos: StateFlow<List<FieldPhoto>> = _notedPhotos

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { _notedPhotos.value = repo.getPhotosWithNotes() }
    }

    fun updateNote(photoId: Long, note: String?) {
        viewModelScope.launch {
            repo.updatePhotoNote(photoId, note)
            refresh()
        }
    }
}
