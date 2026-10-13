package com.fieldrecord.presentation.camera;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000l\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u001c\n\u0002\b\u0007\n\u0002\u0010\u0007\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\t\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u000bH\u0002J\u0016\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\u0010J\u0010\u0010\u0011\u001a\u0004\u0018\u00010\u00122\u0006\u0010\u0013\u001a\u00020\u0014J\u0014\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00120\u00162\u0006\u0010\u0013\u001a\u00020\u0014J\u001d\u0010\u0017\u001a\u00020\u00042\u0006\u0010\u0018\u001a\u00020\u00042\u0006\u0010\u0019\u001a\u00020\u001aH\u0000\u00a2\u0006\u0002\b\u001bJ\u0006\u0010\u001c\u001a\u00020\u001dJ\u0006\u0010\u001e\u001a\u00020\u001dJ\u0010\u0010\u001f\u001a\u0004\u0018\u00010\r2\u0006\u0010 \u001a\u00020!J#\u0010\"\u001a\u00020\u001a2\u0006\u0010\u0018\u001a\u00020\u00042\f\u0010#\u001a\b\u0012\u0004\u0012\u00020\u00040$H\u0000\u00a2\u0006\u0002\b%J\u0012\u0010&\u001a\u0004\u0018\u00010\u00122\u0006\u0010\'\u001a\u00020\u001dH\u0002J8\u0010(\u001a\u00020\u00102\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\'\u001a\u00020\u001d2\u0006\u0010)\u001a\u00020\u000b2\u0006\u0010*\u001a\u00020\u000b2\u0006\u0010+\u001a\u00020,2\b\b\u0002\u0010-\u001a\u00020\u001aR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006."}, d2 = {"Lcom/fieldrecord/presentation/camera/PhotoFileManager;", "", "()V", "EXTENSION", "", "PHOTO_FOLDER", "ROOT_FOLDER", "filenamePattern", "Lkotlin/text/Regex;", "convertToDMS", "coord", "", "cropToAspectRatio", "Landroid/graphics/Bitmap;", "bitmap", "isLandscape", "", "findLatestPhoto", "Lcom/fieldrecord/domain/model/FieldPhoto;", "context", "Landroid/content/Context;", "findPhotos", "", "formatFilename", "dateStamp", "sequence", "", "formatFilename$app_debug", "getOutputFile", "Ljava/io/File;", "getStorageDir", "imageProxyToBitmap", "image", "Landroidx/camera/core/ImageProxy;", "nextMonthlySequence", "filenames", "", "nextMonthlySequence$app_debug", "readPhotoMetadata", "file", "saveLocationWithExifAndWatermark", "latitude", "longitude", "bearing", "", "targetRotation", "app_debug"})
public final class PhotoFileManager {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String ROOT_FOLDER = "FieldRecord";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PHOTO_FOLDER = "Photo";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String EXTENSION = ".jpg";
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.text.Regex filenamePattern = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.fieldrecord.presentation.camera.PhotoFileManager INSTANCE = null;
    
    private PhotoFileManager() {
        super();
    }
    
    /**
     * USB-visible shared internal storage: /storage/emulated/0/FieldRecord/Photo.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.io.File getStorageDir() {
        return null;
    }
    
    /**
     * Create a collision-safe monthly sequence in the form IMG_YYMMdd_xxx.jpg.
     */
    @kotlin.jvm.Synchronized()
    @org.jetbrains.annotations.NotNull()
    public final synchronized java.io.File getOutputFile() {
        return null;
    }
    
    public final int nextMonthlySequence$app_debug(@org.jetbrains.annotations.NotNull()
    java.lang.String dateStamp, @org.jetbrains.annotations.NotNull()
    java.lang.Iterable<java.lang.String> filenames) {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String formatFilename$app_debug(@org.jetbrains.annotations.NotNull()
    java.lang.String dateStamp, int sequence) {
        return null;
    }
    
    /**
     * Scan active and known legacy folders, newest first, for camera preview initialization.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.fieldrecord.domain.model.FieldPhoto> findPhotos(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.fieldrecord.domain.model.FieldPhoto findLatestPhoto(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    private final com.fieldrecord.domain.model.FieldPhoto readPhotoMetadata(java.io.File file) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final android.graphics.Bitmap cropToAspectRatio(@org.jetbrains.annotations.NotNull()
    android.graphics.Bitmap bitmap, boolean isLandscape) {
        return null;
    }
    
    /**
     * Save GPS metadata and apply watermark with white bar and blue text.
     */
    public final boolean saveLocationWithExifAndWatermark(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    java.io.File file, double latitude, double longitude, float bearing, int targetRotation) {
        return false;
    }
    
    private final java.lang.String convertToDMS(double coord) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final android.graphics.Bitmap imageProxyToBitmap(@org.jetbrains.annotations.NotNull()
    androidx.camera.core.ImageProxy image) {
        return null;
    }
}