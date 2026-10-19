package com.fieldrecord.presentation.gallery

import com.fieldrecord.domain.model.FieldPhoto
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryMediaTimelineTest {
    @Test
    fun photosAndVideosShareOneNewestFirstTimeline() {
        val photo = FieldPhoto(1, "/photos/a.jpg", "a.jpg", 0.0, 0.0, -1f, 10, null)
        val olderVideo = GalleryVideoItem("/videos/old.mp4", "old.mp4", 5)
        val newerVideo = GalleryVideoItem("/videos/new.mp4", "new.mp4", 15)

        val timeline = buildGalleryTimeline(listOf(photo), listOf(olderVideo, newerVideo))

        assertEquals(
            listOf("video:new.mp4", "photo:1", "video:old.mp4"),
            timeline.map { it.key }
        )
    }

    @Test
    fun videoDurationLabelsIncludeHoursWhenNeeded() {
        assertEquals("01:05", formatVideoDurationLabel(65_000))
        assertEquals("01:01:05", formatVideoDurationLabel(3_665_000))
        assertEquals("00:00", formatVideoDurationLabel(-1))
    }

    @Test
    fun videoRangeFilterIncludesBothBoundaryTimestamps() {
        val videos = listOf(
            GalleryVideoItem("/videos/before.mp4", "before.mp4", 99),
            GalleryVideoItem("/videos/start.mp4", "start.mp4", 100),
            GalleryVideoItem("/videos/end.mp4", "end.mp4", 200),
            GalleryVideoItem("/videos/after.mp4", "after.mp4", 201)
        )

        assertEquals(
            listOf("start.mp4", "end.mp4"),
            videosInDateRange(videos, 100, 200).map { it.fileName }
        )
    }
}
