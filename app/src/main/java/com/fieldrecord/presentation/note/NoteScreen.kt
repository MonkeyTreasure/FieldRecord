package com.fieldrecord.presentation.note

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fieldrecord.domain.model.FieldPhoto
import com.fieldrecord.domain.model.extractPhotoTags
import com.fieldrecord.domain.model.photoTagSuggestions
import com.fieldrecord.domain.model.applyPhotoTagSuggestion
import com.fieldrecord.domain.model.photoTagColorArgb
import com.fieldrecord.presentation.PhotoNoteText
import com.fieldrecord.presentation.photo.PhotoFullscreenViewer
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NoteScreen(
    viewModel: NoteViewModel = hiltViewModel()
) {
    val photos by viewModel.notedPhotos.collectAsState()
    var query by remember { mutableStateOf("") }
    var editPhoto by remember { mutableStateOf<FieldPhoto?>(null) }
    var editNote by remember { mutableStateOf("") }
    var viewerPhotos by remember { mutableStateOf<List<FieldPhoto>?>(null) }
    var viewerIndex by remember { mutableIntStateOf(0) }
    val knownTags = remember(photos) { photos.flatMap { extractPhotoTags(it.note) }.toSet() }
    val noteTagSuggestions = photoTagSuggestions(editNote, knownTags)
    val filtered = remember(photos, query) {
        if (query.isBlank()) photos else photos.filter {
            (it.note?.contains(query, true) == true) || it.fileName.contains(query, true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // header fixed
        Surface(elevation = 4.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Catatan Foto", style = MaterialTheme.typography.subtitle1)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cari note / filename", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${filtered.size} foto bernote", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top=4.dp))
            }
        }
        if (filtered.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Belum ada catatan", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { photo ->
                    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(photo.fileName, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(photo.timestamp)), fontSize = 11.sp, color = Color.Gray)
                                Spacer(Modifier.height(4.dp))
                                PhotoNoteText(
                                    note = photo.note.orEmpty(),
                                    defaultColor = Color.Black,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.body2,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { editPhoto = photo; editNote = photo.note ?: "" }) {
                                Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF2E7D32))
                            }
                        }
                    }
                }
            }
        }
    }

    editPhoto?.let { p ->
        AlertDialog(
            onDismissRequest = { editPhoto = null },
            title = { Text("Edit catatan") },
            text = {
                OutlinedTextField(value = editNote, onValueChange = { editNote = it }, placeholder = { Text("Catatan; gunakan #tag untuk kategori") }, modifier = Modifier.fillMaxWidth())
                if (noteTagSuggestions.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        noteTagSuggestions.forEach { tag ->
                            TextButton(onClick = { editNote = applyPhotoTagSuggestion(editNote, tag) }) {
                                Text("#$tag", color = Color(photoTagColorArgb(tag)))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateNote(p.id, editNote.ifBlank { null })
                    editPhoto = null
                }) { Text("Simpan", color = Color(0xFF2E7D32)) }
            },
            dismissButton = { TextButton(onClick = { editPhoto = null }) { Text("Batal") } }
        )
    }

    viewerPhotos?.let { list ->
        PhotoFullscreenViewer(photos = list, initialIndex = viewerIndex, knownTags = knownTags, onDismiss = { viewerPhotos = null }, onSaveNote = { id, n -> viewModel.updateNote(id, n) })
    }
}
