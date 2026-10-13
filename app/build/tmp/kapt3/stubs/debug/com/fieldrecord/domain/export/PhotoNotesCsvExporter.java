package com.fieldrecord.domain.export;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0004\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0014\u0010\u0005\u001a\u00020\u00042\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007J\u0010\u0010\t\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u000bH\u0002J\u0010\u0010\f\u001a\u00020\u00042\u0006\u0010\r\u001a\u00020\u0004H\u0002J\f\u0010\u000e\u001a\u00020\u0004*\u00020\u0004H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lcom/fieldrecord/domain/export/PhotoNotesCsvExporter;", "", "()V", "HEADER", "", "export", "photos", "", "Lcom/fieldrecord/domain/model/FieldPhoto;", "formatTimestamp", "timestamp", "", "safeSpreadsheetText", "value", "toCsvCell", "app_debug"})
public final class PhotoNotesCsvExporter {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String HEADER = "photo_id,photo_filename,captured_at,latitude,longitude,bearing,note";
    @org.jetbrains.annotations.NotNull()
    public static final com.fieldrecord.domain.export.PhotoNotesCsvExporter INSTANCE = null;
    
    private PhotoNotesCsvExporter() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String export(@org.jetbrains.annotations.NotNull()
    java.util.List<com.fieldrecord.domain.model.FieldPhoto> photos) {
        return null;
    }
    
    private final java.lang.String safeSpreadsheetText(java.lang.String value) {
        return null;
    }
    
    private final java.lang.String toCsvCell(java.lang.String $this$toCsvCell) {
        return null;
    }
    
    private final java.lang.String formatTimestamp(long timestamp) {
        return null;
    }
}