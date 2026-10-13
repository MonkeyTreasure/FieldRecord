package com.fieldrecord.domain.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val hashtagRegex = Regex("(?<![\\p{L}\\p{N}_])#([\\p{L}\\p{N}_-]+)")

/** Returns canonical, case-insensitive hashtag categories from a photo note. */
fun extractPhotoTags(note: String?): Set<String> =
    hashtagRegex.findAll(note.orEmpty())
        .map { it.groupValues[1].lowercase(Locale.ROOT) }
        .toSet()

/** Stable, automatically assigned color for a hashtag; case variants share one color. */
fun photoTagColorArgb(tag: String): Int {
    val normalized = tag.trim().lowercase(Locale.ROOT)
    val hue = 45L + (normalized.hashCode().toLong() and 0xFFFFFFFFL) % 270L
    val chroma = 0.82f * 0.72f
    val sectorPosition = hue / 60f
    val intermediate = chroma * (1f - abs(sectorPosition % 2f - 1f))
    val (red, green, blue) = when (hue / 60L) {
        0L -> Triple(chroma, intermediate, 0f)
        1L -> Triple(intermediate, chroma, 0f)
        2L -> Triple(0f, chroma, intermediate)
        3L -> Triple(0f, intermediate, chroma)
        4L -> Triple(intermediate, 0f, chroma)
        else -> Triple(chroma, 0f, intermediate)
    }
    val offset = 0.82f - chroma
    fun channel(value: Float) = ((value + offset) * 255f).roundToInt().coerceIn(0, 255)
    return (0xFF shl 24) or (channel(red) shl 16) or (channel(green) shl 8) or channel(blue)
}

/** Colors a single photo pin from the first hashtag in its saved note. */
fun photoPinColorArgb(note: String?): Int =
    extractPhotoTags(note).firstOrNull()?.let(::photoTagColorArgb) ?: 0xFFDC2D2D.toInt()

/** Chooses the dominant category color for a cluster; untagged photos remain red. */
fun photoMarkerColorArgb(tags: List<String?>): Int {
    val dominantTag = tags
        .filterNotNull()
        .map { it.trim().lowercase(Locale.ROOT) }
        .filter { it.isNotEmpty() }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .firstOrNull()
        ?.key
    return dominantTag?.let(::photoTagColorArgb) ?: 0xFFDC2D2D.toInt()
}

/** Existing categories that match the hashtag currently being typed. */
fun photoTagSuggestions(note: String, knownTags: Set<String>): List<String> {
    val token = note.substring(note.indexOfLast { it.isWhitespace() } + 1).takeIf { it.startsWith('#') } ?: return emptyList()
    val prefix = token.drop(1).lowercase(Locale.ROOT)
    return knownTags.filter { it.startsWith(prefix) && it != prefix }.sorted()
}

/** Replace the unfinished hashtag token with the selected canonical category. */
fun applyPhotoTagSuggestion(note: String, tag: String): String {
    val boundary = note.indexOfLast { it.isWhitespace() }
    return note.substring(0, boundary + 1) + "#$tag "
}
