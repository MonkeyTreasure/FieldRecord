package com.fieldrecord.domain.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val hashtagRegex = Regex("(?<![\\p{L}\\p{N}_])#([\\p{L}\\p{N}_-]+)")

enum class PhotoTagKind { CATEGORY, BLOCK, LOCATION }

data class PhotoTagOption(val key: String, val label: String)

data class PhotoTagFacets(
    val categories: List<PhotoTagOption> = emptyList(),
    val blocks: List<PhotoTagOption> = emptyList(),
    val locations: List<PhotoTagOption> = emptyList()
) {
    val isEmpty: Boolean get() = categories.isEmpty() && blocks.isEmpty() && locations.isEmpty()
}

data class PhotoTagFilter(val kind: PhotoTagKind, val key: String, val label: String = key)

/** Classifies normal hashtags as categories and #blok_CODE_LOCATION tags as block/location facets. */
fun extractPhotoTagFacets(note: String?): PhotoTagFacets {
    val categories = linkedMapOf<String, PhotoTagOption>()
    val blocks = linkedMapOf<String, PhotoTagOption>()
    val locations = linkedMapOf<String, PhotoTagOption>()

    fun normalized(value: String) = value.trim().lowercase(Locale.ROOT)
    fun add(target: MutableMap<String, PhotoTagOption>, value: String, label: String = value) {
        val key = normalized(value)
        if (key.isNotEmpty()) target.putIfAbsent(key, PhotoTagOption(key, label))
    }
    fun titleWords(value: String) = value.split('_')
        .filter(String::isNotBlank)
        .joinToString(" ") { word -> word.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) } }

    hashtagRegex.findAll(note.orEmpty()).forEach { match ->
        val rawTag = match.groupValues[1]
        if (rawTag.startsWith("blok_", ignoreCase = true)) {
            val blockAndLocation = rawTag.substringAfter('_')
            val separator = blockAndLocation.indexOf('_')
            val blockCode = if (separator < 0) blockAndLocation else blockAndLocation.substring(0, separator)
            val location = if (separator < 0) "" else blockAndLocation.substring(separator + 1)
            add(blocks, blockCode)
            add(locations, location, location.replace('_', ' '))
        } else {
            add(categories, rawTag, titleWords(rawTag))
        }
    }

    fun sorted(values: Map<String, PhotoTagOption>) = values.values.sortedBy { it.label.lowercase(Locale.ROOT) }
    return PhotoTagFacets(sorted(categories), sorted(blocks), sorted(locations))
}

/** Combines facets from saved photo notes, deduplicating values case-insensitively. */
fun collectPhotoTagFacets(notes: Iterable<String?>): PhotoTagFacets {
    val categories = linkedMapOf<String, PhotoTagOption>()
    val blocks = linkedMapOf<String, PhotoTagOption>()
    val locations = linkedMapOf<String, PhotoTagOption>()
    notes.forEach { note ->
        val facets = extractPhotoTagFacets(note)
        facets.categories.forEach { categories.putIfAbsent(it.key, it) }
        facets.blocks.forEach { blocks.putIfAbsent(it.key, it) }
        facets.locations.forEach { locations.putIfAbsent(it.key, it) }
    }
    fun sorted(values: Map<String, PhotoTagOption>) = values.values.sortedBy { it.label.lowercase(Locale.ROOT) }
    return PhotoTagFacets(sorted(categories), sorted(blocks), sorted(locations))
}

fun matchesPhotoTagFilter(note: String?, filter: PhotoTagFilter): Boolean {
    val facets = extractPhotoTagFacets(note)
    val options = when (filter.kind) {
        PhotoTagKind.CATEGORY -> facets.categories
        PhotoTagKind.BLOCK -> facets.blocks
        PhotoTagKind.LOCATION -> facets.locations
    }
    return options.any { it.key == filter.key.trim().lowercase(Locale.ROOT) }
}

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
fun photoPinColorArgb(note: String?, colorForTag: (String) -> Int = ::photoTagColorArgb): Int =
    extractPhotoTags(note).firstOrNull()?.let(colorForTag) ?: 0xFFDC2D2D.toInt()

/** Chooses the dominant category color for a cluster; untagged photos remain red. */
fun photoMarkerColorArgb(tags: List<String?>, colorForTag: (String) -> Int = ::photoTagColorArgb): Int {
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
    return dominantTag?.let(colorForTag) ?: 0xFFDC2D2D.toInt()
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
