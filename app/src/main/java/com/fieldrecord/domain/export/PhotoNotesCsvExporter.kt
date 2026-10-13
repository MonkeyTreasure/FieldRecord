package com.fieldrecord.domain.export

import com.fieldrecord.domain.model.FieldPhoto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PhotoNotesCsvExporter {
    private const val HEADER = "photo_id,photo_filename,captured_at,latitude,longitude,bearing,note"

    fun export(photos: List<FieldPhoto>): String = buildString {
        append(HEADER).append("\r\n")
        photos.asSequence()
            .filter { !it.note.isNullOrBlank() }
            .sortedBy { it.timestamp }
            .forEach { photo ->
                val values = listOf(
                    photo.id.toString(),
                    safeSpreadsheetText(photo.fileName),
                    formatTimestamp(photo.timestamp),
                    photo.latitude.toString(),
                    photo.longitude.toString(),
                    photo.bearing.toString(),
                    safeSpreadsheetText(photo.note.orEmpty())
                )
                append(values.joinToString(",") { it.toCsvCell() }).append("\r\n")
            }
    }

    private fun safeSpreadsheetText(value: String): String {
        val firstMeaningful = value.dropWhile(Char::isWhitespace).firstOrNull()
        return if (firstMeaningful in setOf('=', '+', '-', '@')) "'$value" else value
    }

    private fun String.toCsvCell(): String {
        val escaped = replace("\"", "\"\"")
        return if (any { it == ',' || it == '"' || it == '\r' || it == '\n' }) "\"$escaped\"" else escaped
    }

    private fun formatTimestamp(timestamp: Long): String = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        Locale.US
    ).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(timestamp))
}
