package com.fieldrecord.presentation.gallery

import androidx.compose.runtime.Composable
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.presentation.photo.PhotoFullscreenViewer

/** Compatibility wrapper; all photo previews share the same viewer presentation. */
@Composable
fun FullscreenViewerScreen(
    photos: List<FieldPhoto>,
    initialIndex: Int,
    onSaveNote: suspend (photoId: Long, note: String?) -> Unit,
    onDismiss: () -> Unit
) {
    PhotoFullscreenViewer(
        photos = photos.sortedBy { it.timestamp },
        initialIndex = initialIndex,
        onSaveNote = onSaveNote,
        onDismiss = onDismiss
    )
}
