package com.fieldrecord

import android.app.Application
import android.os.Environment
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class FieldRecordApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        MapLibre.getInstance(this)
    }

    /** Simpan stacktrace force close ke Internal storage/FieldRecord/crash_log.txt (+ fallback app files). */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            val text = buildString {
                append("=== ")
                append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                append(" thread=").append(thread.name).append('\n')
                append(android.util.Log.getStackTraceString(error)).append('\n')
            }
            listOf(
                File(Environment.getExternalStorageDirectory(), "FieldRecord/crash_log.txt"),
                File(filesDir, "crash_log.txt")
            ).forEach { file ->
                runCatching {
                    file.parentFile?.mkdirs()
                    file.appendText(text)
                }
            }
            previous?.uncaughtException(thread, error)
        }
    }
}
