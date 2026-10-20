package com.fieldrecord.presentation.gallery

import com.fieldrecord.domain.model.FieldPhoto

internal data class GalleryVideoItem(
    val filePath: String,
    val fileName: String,
    val timestamp: Long
)

internal sealed class GalleryMediaItem {
    abstract val key: String
    abstract val timestamp: Long

    data class Photo(val photo: FieldPhoto) : GalleryMediaItem() {
        override val key: String = "photo:${photo.id}"
        override val timestamp: Long = photo.timestamp
    }

    data class Video(val video: GalleryVideoItem) : GalleryMediaItem() {
        override val key: String = "video:${video.fileName}"
        override val timestamp: Long = video.timestamp
    }
}

internal fun gallerySwipeTargetIndex(currentIndex: Int, itemCount: Int, swipeLeft: Boolean): Int? {
    if (itemCount <= 0 || currentIndex !in 0 until itemCount) return null
    val target = currentIndex + if (swipeLeft) 1 else -1
    return target.takeIf { it in 0 until itemCount }
}

internal fun buildGalleryTimeline(
    photos: List<FieldPhoto>,
    videos: List<GalleryVideoItem>
): List<GalleryMediaItem> =
    (photos.map { GalleryMediaItem.Photo(it) } + videos.map { GalleryMediaItem.Video(it) })
        .sortedByDescending { it.timestamp }

internal fun videosInDateRange(
    videos: List<GalleryVideoItem>,
    startInclusive: Long,
    endInclusive: Long
): List<GalleryVideoItem> = videos.filter { it.timestamp in startInclusive..endInclusive }

internal fun formatVideoDurationLabel(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1_000L).toInt()
    return if (totalSeconds >= 3_600) {
        "%02d:%02d:%02d".format(totalSeconds / 3_600, (totalSeconds / 60) % 60, totalSeconds % 60)
    } else {
        "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }
}
