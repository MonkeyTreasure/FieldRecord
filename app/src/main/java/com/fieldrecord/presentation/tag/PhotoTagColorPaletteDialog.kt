package com.fieldrecord.presentation.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fieldrecord.data.local.PhotoTagColorStore

@Composable
fun rememberPhotoTagColorStore(): PhotoTagColorStore {
    val context = LocalContext.current
    return remember(context) { PhotoTagColorStore(context) }
}

@Composable
fun PhotoTagColorPaletteDialog(
    tag: String,
    colors: List<Int>,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF673AB7), elevation = 12.dp) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.Start) {
                Text("Warna #$tag", color = Color.White, style = MaterialTheme.typography.h6)
                Spacer(Modifier.size(8.dp))
                Text("Pilih warna yang belum digunakan", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.body2)
                Spacer(Modifier.size(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEachIndexed { index, argb ->
                        val description = "Pilihan warna ${index + 1}"
                        Spacer(
                            Modifier
                                .size(44.dp)
                                .background(Color(argb), CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                                .clickable { onColorSelected(argb) }
                                .semantics { contentDescription = description }
                        )
                    }
                }
                Spacer(Modifier.size(8.dp))
                Text("Warna lama tidak akan digunakan kembali.", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.caption)
            }
        }
    }
}
