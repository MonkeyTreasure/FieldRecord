package com.fieldrecord.presentation.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.fieldrecord.LocalAppIconColor
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.domain.model.PhotoTagFilter
import com.fieldrecord.domain.model.matchesPhotoTagFilter
import com.fieldrecord.presentation.photo.PhotoFullscreenViewer
import java.io.File

@Composable
fun TagPhotoGalleryScreen(
    photos: List<FieldPhoto>,
    filter: PhotoTagFilter,
    onBack: () -> Unit,
    onSaveNote: (Long, String?) -> Unit
) {
    val filteredPhotos = remember(photos, filter) {
        photos.filter { matchesPhotoTagFilter(it.note, filter) }.sortedByDescending { it.timestamp }
    }
    var viewerIndex by remember { mutableIntStateOf(-1) }
    val chronologicalPhotos = remember(filteredPhotos) { filteredPhotos.sortedBy { it.timestamp } }

    BackHandler(enabled = viewerIndex >= 0) { viewerIndex = -1 }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colors.surface).padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali ke dashboard", tint = LocalAppIconColor.current)
            }
            Column(Modifier.weight(1f)) {
                Text(filter.label, style = MaterialTheme.typography.subtitle1, color = MaterialTheme.colors.onSurface)
                Text("${filteredPhotos.size} foto bertag", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
            }
        }
        if (filteredPhotos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada foto dengan tag ini", color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(filteredPhotos, key = { _, photo -> photo.id }) { index, photo ->
                    Image(
                        painter = rememberAsyncImagePainter(File(photo.filePath)),
                        contentDescription = photo.fileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable { viewerIndex = index }
                    )
                }
            }
        }
    }
    if (viewerIndex >= 0) {
        val selectedPhotoId = filteredPhotos[viewerIndex].id
        PhotoFullscreenViewer(
            photos = chronologicalPhotos,
            initialIndex = chronologicalPhotos.indexOfFirst { it.id == selectedPhotoId }.coerceAtLeast(0),
            knownTags = filteredPhotos.flatMap { com.fieldrecord.domain.model.extractPhotoTags(it.note) }.toSet(),
            onDismiss = { viewerIndex = -1 },
            onSaveNote = onSaveNote
        )
    }
}
