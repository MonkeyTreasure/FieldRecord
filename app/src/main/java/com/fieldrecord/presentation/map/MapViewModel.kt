package com.fieldrecord.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fieldrecord.data.local.dao.TrackDao
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.domain.repository.FieldPhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: FieldPhotoRepository,
    private val trackDao: TrackDao
) : ViewModel() {
    val photos: StateFlow<List<FieldPhoto>> = repository.getAllPhotos().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    val latestTrack: StateFlow<TrackSnapshot?> = trackDao.observeLatestSession()
        .flatMapLatest { session ->
            if (session == null) flowOf(null)
            else trackDao.observePoints(session.id).map { points -> TrackSnapshot(session, points) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun updatePhotoNote(photoId: Long, note: String?) {
        repository.updatePhotoNote(photoId, note)
    }
}
