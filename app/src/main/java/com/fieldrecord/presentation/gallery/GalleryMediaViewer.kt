package com.fieldrecord.presentation.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fieldrecord.presentation.photo.PhotoFullscreenViewer
import com.fieldrecord.presentation.video.VideoPlayerDialog
import java.io.File

@Composable
internal fun GalleryMediaViewer(
    timeline: List<GalleryMediaItem>,
    initialIndex: Int,
    knownTags: Set<String>,
    onSaveNote: suspend (photoId: Long, note: String?) -> Unit,
    onDismiss: () -> Unit
) {
    if (timeline.isEmpty()) return

    var currentIndex by remember(timeline) {
        mutableIntStateOf(initialIndex.coerceIn(0, timeline.lastIndex))
    }
    LaunchedEffect(timeline.size) {
        currentIndex = currentIndex.coerceIn(0, timeline.lastIndex)
    }

    val swipeLeft: () -> Unit = remember(timeline, currentIndex) {
        {
            gallerySwipeTargetIndex(currentIndex, timeline.size, swipeLeft = true)
                ?.let { currentIndex = it }
        }
    }
    val swipeRight: () -> Unit = remember(timeline, currentIndex) {
        {
            gallerySwipeTargetIndex(currentIndex, timeline.size, swipeLeft = false)
                ?.let { currentIndex = it }
        }
    }

    when (val media = timeline[currentIndex]) {
        is GalleryMediaItem.Photo -> PhotoFullscreenViewer(
            photos = listOf(media.photo),
            knownTags = knownTags,
            displayIndex = currentIndex,
            displayTotal = timeline.size,
            onSwipeLeft = swipeLeft,
            onSwipeRight = swipeRight,
            onSaveNote = onSaveNote,
            onDismiss = onDismiss
        )
        is GalleryMediaItem.Video -> VideoPlayerDialog(
            file = File(media.video.filePath),
            onDismiss = onDismiss,
            onSwipeLeft = swipeLeft,
            onSwipeRight = swipeRight
        )
    }
}
