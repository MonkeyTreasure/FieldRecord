package com.fieldrecord.presentation.note

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.domain.model.extractPhotoTags
import com.fieldrecord.presentation.PhotoNoteText
import com.fieldrecord.presentation.photo.PhotoFullscreenViewer

private val photoSequencePattern = Regex("^IMG_\\d{6}_(\\d{3,})(?:_|\\.)", RegexOption.IGNORE_CASE)
private val noteHashtagPattern = Regex("(?<![\\p{L}\\p{N}_])#[\\p{L}\\p{N}_-]+")

@Composable
fun NoteScreen(
    photos: List<FieldPhoto>,
    onSaveNote: suspend (photoId: Long, note: String?) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var viewerPhotos by remember { mutableStateOf<List<FieldPhoto>?>(null) }
    var viewerIndex by remember { mutableIntStateOf(0) }
    val notedPhotos = remember(photos) {
        photos.filter { !it.note.isNullOrBlank() }.sortedByDescending { it.timestamp }
    }
    val filteredPhotos = remember(notedPhotos, query) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) notedPhotos else notedPhotos.filter { photo ->
            val tags = extractPhotoTags(photo.note).joinToString(" ") { "#$it" }
            val sequence = imageSeries(photo.fileName)
            photo.note.orEmpty().contains(normalizedQuery, ignoreCase = true) ||
                tags.contains(normalizedQuery, ignoreCase = true) ||
                photo.fileName.contains(normalizedQuery, ignoreCase = true) ||
                sequence.contains(normalizedQuery, ignoreCase = true)
        }
    }
    val knownTags = remember(photos) { photos.flatMap { extractPhotoTags(it.note) }.toSet() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .statusBarsPadding()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            "Noted",
            color = Color.White,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)
        )
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(color = Color.White),
            placeholder = { Text("Search notes, tags, or image series…", color = Color.White.copy(alpha = 0.75f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search notes", tint = Color.White) },
            trailingIcon = if (query.isNotEmpty()) ({
                IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Color.White)
                }
            }) else null,
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color(0xFF808080).copy(alpha = 0.5f),
                textColor = Color.White,
                cursorColor = Color.White,
                placeholderColor = Color.White.copy(alpha = 0.75f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF512DA8), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Image series", modifier = Modifier.weight(0.85f), color = Color.White, fontSize = 11.sp, maxLines = 2)
            Text("Tag", modifier = Modifier.weight(1.35f), color = Color.White, fontSize = 11.sp)
            Text("Note", modifier = Modifier.weight(1.8f), color = Color.White, fontSize = 11.sp)
        }
        if (filteredPhotos.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (notedPhotos.isEmpty()) "No photo notes yet" else "No matching notes",
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                itemsIndexed(filteredPhotos, key = { _, photo -> photo.id }) { index, photo ->
                    val tags = extractPhotoTags(photo.note).joinToString(" ") { "#$it" }.ifBlank { "—" }
                    val noteText = noteHashtagPattern.replace(photo.note.orEmpty(), " ").replace(Regex("\\s+"), " ").trim().ifBlank { "—" }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF673AB7), RoundedCornerShape(6.dp))
                            .clickable {
                                viewerPhotos = filteredPhotos
                                viewerIndex = index
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            imageSeries(photo.fileName),
                            modifier = Modifier.weight(0.85f),
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (tags == "—") {
                            Text("—", modifier = Modifier.weight(1.35f), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        } else {
                            PhotoNoteText(
                                note = tags,
                                modifier = Modifier.weight(1.35f),
                                defaultColor = Color.White,
                                style = MaterialTheme.typography.body2.copy(fontSize = 11.sp),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            noteText,
                            modifier = Modifier.weight(1.8f),
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    viewerPhotos?.let { list ->
        PhotoFullscreenViewer(
            photos = list,
            initialIndex = viewerIndex,
            knownTags = knownTags,
            onDismiss = { viewerPhotos = null },
            onSaveNote = onSaveNote
        )
    }
}

private fun imageSeries(fileName: String): String =
    photoSequencePattern.find(fileName)?.groupValues?.getOrNull(1) ?: "—"
