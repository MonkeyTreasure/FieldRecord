package com.fieldrecord.presentation.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import java.io.File

internal data class VideoPreviewInfo(val bitmap: Bitmap?, val durationMs: Long)

internal fun loadVideoPreview(file: File): VideoPreviewInfo? {
    if (!file.isFile || file.length() == 0L) return null
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 480, 480)
        } else {
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let(::scaleBitmap)
        }
        VideoPreviewInfo(frame, duration)
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun scaleBitmap(bitmap: Bitmap): Bitmap {
    val scale = minOf(1f, 480f / maxOf(bitmap.width, bitmap.height))
    if (scale >= 1f) return bitmap
    val scaled = Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1),
        true
    )
    if (scaled !== bitmap) bitmap.recycle()
    return scaled
}
