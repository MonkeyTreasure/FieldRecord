package com.fieldrecord.presentation.map;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000R\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a\u0081\u0001\u0010\u0000\u001a\u00020\u00012\b\b\u0002\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\u0012\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\b2\u001a\b\u0002\u0010\t\u001a\u0014\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\u0005\u0012\u0004\u0012\u00020\u00010\b2*\u0010\u000b\u001a&\b\u0001\u0012\u0004\u0012\u00020\n\u0012\u0006\u0012\u0004\u0018\u00010\r\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00010\u000e\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\fH\u0007\u00a2\u0006\u0002\u0010\u0010\u001a\u0018\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0014H\u0002\u001a\u0018\u0010\u0016\u001a\u00020\u00122\u0006\u0010\u0015\u001a\u00020\u00142\u0006\u0010\u0017\u001a\u00020\u0014H\u0002\u001a&\u0010\u0018\u001a\u00020\u00012\u0006\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u001c2\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005H\u0002\u00a8\u0006\u001d"}, d2 = {"MapScreen", "", "modifier", "Landroidx/compose/ui/Modifier;", "photos", "", "Lcom/fieldrecord/domain/model/FieldPhoto;", "onPhotoClick", "Lkotlin/Function1;", "onClusterClick", "", "onSavePhotoNote", "Lkotlin/Function3;", "", "Lkotlin/coroutines/Continuation;", "", "(Landroidx/compose/ui/Modifier;Ljava/util/List;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function3;)V", "createClusterBitmap", "Landroid/graphics/Bitmap;", "count", "", "color", "createDotBitmap", "size", "renderPhotoMarkers", "map", "Lorg/maplibre/android/maps/MapLibreMap;", "context", "Landroid/content/Context;", "app_debug"})
public final class MapScreenKt {
    
    @androidx.compose.runtime.Composable()
    public static final void MapScreen(@org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, @org.jetbrains.annotations.NotNull()
    java.util.List<com.fieldrecord.domain.model.FieldPhoto> photos, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.fieldrecord.domain.model.FieldPhoto, kotlin.Unit> onPhotoClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.util.List<java.lang.Long>, kotlin.Unit> onClusterClick, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function3<? super java.lang.Long, ? super java.lang.String, ? super kotlin.coroutines.Continuation<? super kotlin.Unit>, ? extends java.lang.Object> onSavePhotoNote) {
    }
    
    private static final void renderPhotoMarkers(org.maplibre.android.maps.MapLibreMap map, android.content.Context context, java.util.List<com.fieldrecord.domain.model.FieldPhoto> photos) {
    }
    
    private static final android.graphics.Bitmap createClusterBitmap(int count, int color) {
        return null;
    }
    
    private static final android.graphics.Bitmap createDotBitmap(int color, int size) {
        return null;
    }
}