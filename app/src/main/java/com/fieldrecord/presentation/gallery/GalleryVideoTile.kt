package com.fieldrecord.presentation.gallery

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import com.fieldrecord.presentation.video.VideoPreviewInfo
import com.fieldrecord.presentation.video.loadVideoPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GalleryVideoTile(
    video: GalleryVideoItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val preview by produceState<VideoPreviewInfo?>(initialValue = null, key1 = video.filePath) {
        value = withContext(Dispatchers.IO) { loadVideoPreview(File(video.filePath)) }
    }
    val thumbnail = preview?.bitmap
    DisposableEffect(thumbnail) {
        onDispose { thumbnail?.takeUnless(Bitmap::isRecycled)?.recycle() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = 2.dp
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xFF202020))) {
            if (thumbnail != null && !thumbnail.isRecycled) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = "Video ${video.fileName}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Putar video ${video.fileName}",
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(38.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50)).padding(6.dp)
            )
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
            ) {
                Text(
                    text = formatVideoDurationLabel(preview?.durationMs ?: 0L),
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}
