package com.fieldrecord.data.local

import android.content.Context
import com.fieldrecord.domain.model.PhotoTagPalette
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists unique tag colors and the colors already consumed by this app installation. */
class PhotoTagColorStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun colorFor(tag: String): Int = synchronized(lock) {
        val key = normalize(tag)
        if (key.isEmpty()) return@synchronized UNTAGGED_COLOR
        val assignments = readAssignments()
        assignments[key]?.let { return@synchronized it }
        val used = readUsedColors().apply { addAll(assignments.values) }
        val color = PhotoTagPalette.nextUnused(used)
        assignments[key] = color
        used += color
        persist(assignments, used)
        color
    }

    fun ensureTags(tags: Iterable<String>) = synchronized(lock) {
        val assignments = readAssignments()
        val used = readUsedColors().apply { addAll(assignments.values) }
        var changed = false
        tags.map(::normalize).filter(String::isNotEmpty).distinct().sorted().forEach { key ->
            if (key !in assignments) {
                val color = PhotoTagPalette.nextUnused(used)
                assignments[key] = color
                used += color
                changed = true
            }
        }
        if (changed) persist(assignments, used)
    }

    fun availableColors(limit: Int = 12): List<Int> = synchronized(lock) {
        val assignments = readAssignments()
        val used = readUsedColors().apply { addAll(assignments.values) }
        PhotoTagPalette.availableColors(used, limit)
    }

    /** A color is accepted only once; changing a tag never releases its previous color. */
    fun assign(tag: String, color: Int): Boolean = synchronized(lock) {
        val key = normalize(tag)
        if (key.isEmpty()) return@synchronized false
        val assignments = readAssignments()
        val current = assignments[key]
        if (current == color) return@synchronized true
        val used = readUsedColors().apply { addAll(assignments.values) }
        if (!PhotoTagPalette.isAvailable(color, used)) return@synchronized false
        assignments[key] = color
        used += color
        persist(assignments, used)
        true
    }

    private fun normalize(tag: String): String = tag.trim().removePrefix("#").lowercase(Locale.ROOT)

    private fun readAssignments(): MutableMap<String, Int> {
        val result = linkedMapOf<String, Int>()
        val raw = preferences.getString(ASSIGNMENTS, null) ?: return result
        runCatching {
            val json = JSONObject(raw)
            json.keys().forEach { key -> result[normalize(key)] = json.getInt(key) }
        }
        return result
    }

    private fun readUsedColors(): MutableSet<Int> {
        val result = mutableSetOf<Int>()
        val raw = preferences.getString(USED_COLORS, null) ?: return result
        runCatching {
            val json = JSONArray(raw)
            for (index in 0 until json.length()) result += json.getInt(index)
        }
        return result
    }

    private fun persist(assignments: Map<String, Int>, used: Set<Int>) {
        val assignmentJson = JSONObject()
        assignments.forEach { (tag, color) -> assignmentJson.put(tag, color) }
        val usedJson = JSONArray()
        used.forEach(usedJson::put)
        preferences.edit()
            .putString(ASSIGNMENTS, assignmentJson.toString())
            .putString(USED_COLORS, usedJson.toString())
            .apply()
        _paletteRevision.value += 1
    }

    companion object {
        private const val PREFERENCES = "photo_tag_colors_v1"
        private const val ASSIGNMENTS = "assignments"
        private const val USED_COLORS = "used_colors"
        private const val UNTAGGED_COLOR = 0xFFDC2D2D.toInt()
        private val lock = Any()
        private val _paletteRevision = MutableStateFlow(0L)
        val paletteRevision: StateFlow<Long> = _paletteRevision.asStateFlow()
    }
}
