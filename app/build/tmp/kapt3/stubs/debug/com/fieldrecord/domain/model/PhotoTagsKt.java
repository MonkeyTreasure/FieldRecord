package com.fieldrecord.domain.model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000$\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\"\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010 \n\u0002\b\u0005\u001a\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u0003\u001a\u0016\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00030\u00072\b\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u001a\u0016\u0010\b\u001a\u00020\t2\u000e\u0010\n\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00030\u000b\u001a\u0010\u0010\f\u001a\u00020\t2\b\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u001a\u000e\u0010\r\u001a\u00020\t2\u0006\u0010\u0005\u001a\u00020\u0003\u001a\"\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00030\u000b2\u0006\u0010\u0004\u001a\u00020\u00032\f\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00030\u0007\"\u000e\u0010\u0000\u001a\u00020\u0001X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"hashtagRegex", "Lkotlin/text/Regex;", "applyPhotoTagSuggestion", "", "note", "tag", "extractPhotoTags", "", "photoMarkerColorArgb", "", "tags", "", "photoPinColorArgb", "photoTagColorArgb", "photoTagSuggestions", "knownTags", "app_debug"})
public final class PhotoTagsKt {
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.text.Regex hashtagRegex = null;
    
    /**
     * Returns canonical, case-insensitive hashtag categories from a photo note.
     */
    @org.jetbrains.annotations.NotNull()
    public static final java.util.Set<java.lang.String> extractPhotoTags(@org.jetbrains.annotations.Nullable()
    java.lang.String note) {
        return null;
    }
    
    /**
     * Stable, automatically assigned color for a hashtag; case variants share one color.
     */
    public static final int photoTagColorArgb(@org.jetbrains.annotations.NotNull()
    java.lang.String tag) {
        return 0;
    }
    
    /**
     * Colors a single photo pin from the first hashtag in its saved note.
     */
    public static final int photoPinColorArgb(@org.jetbrains.annotations.Nullable()
    java.lang.String note) {
        return 0;
    }
    
    /**
     * Chooses the dominant category color for a cluster; untagged photos remain red.
     */
    public static final int photoMarkerColorArgb(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> tags) {
        return 0;
    }
    
    /**
     * Existing categories that match the hashtag currently being typed.
     */
    @org.jetbrains.annotations.NotNull()
    public static final java.util.List<java.lang.String> photoTagSuggestions(@org.jetbrains.annotations.NotNull()
    java.lang.String note, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.String> knownTags) {
        return null;
    }
    
    /**
     * Replace the unfinished hashtag token with the selected canonical category.
     */
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String applyPhotoTagSuggestion(@org.jetbrains.annotations.NotNull()
    java.lang.String note, @org.jetbrains.annotations.NotNull()
    java.lang.String tag) {
        return null;
    }
}