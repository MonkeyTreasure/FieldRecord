package com.fieldrecord.presentation.photo;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000X\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\"\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a\u001e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00040\bH\u0003\u001a\u008d\u0001\u0010\t\u001a\u00020\u00042\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000b2\b\b\u0002\u0010\r\u001a\u00020\u000e2\u000e\b\u0002\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00060\u00102H\u0010\u0011\u001aD\b\u0001\u0012\u0013\u0012\u00110\u0013\u00a2\u0006\f\b\u0014\u0012\b\b\u0015\u0012\u0004\b\b(\u0016\u0012\u0015\u0012\u0013\u0018\u00010\u0006\u00a2\u0006\f\b\u0014\u0012\b\b\u0015\u0012\u0004\b\b(\u0005\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00040\u0017\u0012\u0006\u0012\u0004\u0018\u00010\u00180\u00122\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00040\bH\u0007\u00a2\u0006\u0002\u0010\u001a\u001a<\u0010\u001b\u001a\u00020\u00042\u0006\u0010\u001c\u001a\u00020\f2\u0006\u0010\u001d\u001a\u00020\u000e2\u0006\u0010\u001e\u001a\u00020\u000e2\f\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\u00040\b2\f\u0010 \u001a\b\u0012\u0004\u0012\u00020\u00040\bH\u0003\u001a\u0018\u0010!\u001a\u00020\u00042\u0006\u0010\"\u001a\u00020#2\u0006\u0010\u001c\u001a\u00020\fH\u0002\u001a\u0010\u0010$\u001a\u00020\u00062\u0006\u0010\u001c\u001a\u00020\fH\u0002\"\u0010\u0010\u0000\u001a\u00020\u0001X\u0082\u0004\u00a2\u0006\u0004\n\u0002\u0010\u0002\u00a8\u0006%"}, d2 = {"Emerald", "Landroidx/compose/ui/graphics/Color;", "J", "NoteBar", "", "note", "", "onEdit", "Lkotlin/Function0;", "PhotoFullscreenViewer", "photos", "", "Lcom/fieldrecord/domain/model/FieldPhoto;", "initialIndex", "", "knownTags", "", "onSaveNote", "Lkotlin/Function3;", "", "Lkotlin/ParameterName;", "name", "photoId", "Lkotlin/coroutines/Continuation;", "", "onDismiss", "(Ljava/util/List;ILjava/util/Set;Lkotlin/jvm/functions/Function3;Lkotlin/jvm/functions/Function0;)V", "PhotoMetadataHeader", "photo", "currentIndex", "total", "onShare", "onClose", "sharePhoto", "context", "Landroid/content/Context;", "viewerPhotoDescription", "app_debug"})
public final class PhotoFullscreenViewerKt {
    private static final long Emerald = 0L;
    
    /**
     * Shared fullscreen photo viewer. Input order must be chronological (oldest first).
     *
     * Layout (atas → bawah):
     *  Header (nama file, tanggal, GPS, index, tombol tutup)
     *  [ruang fleksibel]
     *  Foto (lebar = layar, tinggi = rasio foto)
     *  2 dp
     *  Note (lebar = layar, tombol +/edit kanan)
     *  [ruang fleksibel]
     */
    @androidx.compose.runtime.Composable()
    public static final void PhotoFullscreenViewer(@org.jetbrains.annotations.NotNull()
    java.util.List<com.fieldrecord.domain.model.FieldPhoto> photos, int initialIndex, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.String> knownTags, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function3<? super java.lang.Long, ? super java.lang.String, ? super kotlin.coroutines.Continuation<? super kotlin.Unit>, ? extends java.lang.Object> onSaveNote, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss) {
    }
    
    /**
     * Note selebar layar, tampil di bawah foto hanya jika ada isinya.
     */
    @androidx.compose.runtime.Composable()
    private static final void NoteBar(java.lang.String note, kotlin.jvm.functions.Function0<kotlin.Unit> onEdit) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void PhotoMetadataHeader(com.fieldrecord.domain.model.FieldPhoto photo, int currentIndex, int total, kotlin.jvm.functions.Function0<kotlin.Unit> onShare, kotlin.jvm.functions.Function0<kotlin.Unit> onClose) {
    }
    
    private static final void sharePhoto(android.content.Context context, com.fieldrecord.domain.model.FieldPhoto photo) {
    }
    
    private static final java.lang.String viewerPhotoDescription(com.fieldrecord.domain.model.FieldPhoto photo) {
        return null;
    }
}