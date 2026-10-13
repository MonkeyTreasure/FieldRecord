package com.fieldrecord.location;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\t\u001a\u0004\u0018\u00010\n2\u0006\u0010\u000b\u001a\u00020\fJ\u0016\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\u000f\u001a\u00020\nR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/fieldrecord/location/LocationStore;", "", "()V", "ACCURACY", "", "LAT", "LON", "PREFS", "TIME", "latest", "Landroid/location/Location;", "context", "Landroid/content/Context;", "save", "", "location", "app_debug"})
public final class LocationStore {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREFS = "live_location";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String LAT = "latitude";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String LON = "longitude";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TIME = "timestamp";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String ACCURACY = "accuracy";
    @org.jetbrains.annotations.NotNull()
    public static final com.fieldrecord.location.LocationStore INSTANCE = null;
    
    private LocationStore() {
        super();
    }
    
    public final void save(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    android.location.Location location) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final android.location.Location latest(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
}