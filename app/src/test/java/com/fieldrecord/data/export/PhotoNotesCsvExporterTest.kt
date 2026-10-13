package com.fieldrecord.data.export

import com.fieldrecord.domain.export.PhotoNotesCsvExporter
import com.fieldrecord.domain.model.FieldPhoto
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoNotesCsvExporterTest {
    @Test
    fun exportsOneRowPerPhotoWithANonBlankNoteAndStablePhotoReference() {
        val csv = PhotoNotesCsvExporter.export(
            listOf(
                photo(id = 7, filename = "IMG_260928_001.jpg", note = "   "),
                photo(id = 8, filename = "IMG_260928_002.jpg", note = "Periksa blok, sisi timur")
            )
        )

        assertEquals(
            "photo_id,photo_filename,captured_at,latitude,longitude,bearing,note\r\n" +
                "8,IMG_260928_002.jpg,2026-09-28T04:48:14.000Z,-4.15296,105.43406,0.0,\"Periksa blok, sisi timur\"\r\n",
            csv
        )
    }

    @Test
    fun quotesCsvSpecialCharactersAndProtectsSpreadsheetFormulaText() {
        val csv = PhotoNotesCsvExporter.export(
            listOf(photo(id = 9, filename = "photo,one.jpg", note = "=1+1\nCatatan \"uji\""))
        )

        assertEquals(
            "photo_id,photo_filename,captured_at,latitude,longitude,bearing,note\r\n" +
                "9,\"photo,one.jpg\",2026-09-28T04:48:14.000Z,-4.15296,105.43406,0.0,\"'=1+1\nCatatan \"\"uji\"\"\"\r\n",
            csv
        )
    }

    private fun photo(id: Long, filename: String, note: String) = FieldPhoto(
        id = id,
        filePath = "/photos/$filename",
        fileName = filename,
        latitude = -4.15296,
        longitude = 105.43406,
        bearing = 0f,
        timestamp = 1790570894000L,
        note = note
    )
}
