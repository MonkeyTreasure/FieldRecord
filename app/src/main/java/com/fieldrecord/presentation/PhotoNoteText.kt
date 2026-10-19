package com.fieldrecord.presentation

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.fieldrecord.domain.model.extractPhotoTags
import com.fieldrecord.data.local.PhotoTagColorStore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember

private val hashtagPattern = Regex("(?<![\\p{L}\\p{N}_])#[\\p{L}\\p{N}_-]+")

@Composable
fun PhotoNoteText(
    note: String,
    modifier: Modifier = Modifier,
    defaultColor: Color = Color.White,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.body1,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val context = LocalContext.current
    val colorStore = remember(context) { PhotoTagColorStore(context) }
    val tags = remember(note) { extractPhotoTags(note) }
    colorStore.ensureTags(tags)
    val annotatedNote = buildAnnotatedString {
        var cursor = 0
        hashtagPattern.findAll(note).forEach { match ->
            append(note.substring(cursor, match.range.first))
            val tagColor = Color(colorStore.colorFor(match.value.drop(1)))
            pushStyle(SpanStyle(color = Color.White, background = tagColor))
            append(match.value)
            pop()
            cursor = match.range.last + 1
        }
        append(note.substring(cursor))
    }
    Text(
        text = annotatedNote,
        modifier = modifier,
        color = defaultColor,
        style = style,
        maxLines = maxLines,
        overflow = overflow
    )
}
