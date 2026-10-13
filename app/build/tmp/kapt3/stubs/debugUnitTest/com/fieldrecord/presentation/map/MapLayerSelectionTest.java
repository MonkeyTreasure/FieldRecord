package com.fieldrecord.presentation.map;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001:\u0001\u0005B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H\u0007\u00a8\u0006\u0006"}, d2 = {"Lcom/fieldrecord/presentation/map/MapLayerSelectionTest;", "", "()V", "lastLayerChoiceIsRestoredByNextSelection", "", "MemoryStore", "app_debugUnitTest"})
public final class MapLayerSelectionTest {
    
    public MapLayerSelectionTest() {
        super();
    }
    
    @org.junit.Test()
    public final void lastLayerChoiceIsRestoredByNextSelection() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\b\u0002\u0018\u00002\u00020\u0001B\u000f\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\u0006\u001a\u00020\u0003H\u0016J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\u0005\u001a\u00020\u0003H\u0016R\u000e\u0010\u0005\u001a\u00020\u0003X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\t"}, d2 = {"Lcom/fieldrecord/presentation/map/MapLayerSelectionTest$MemoryStore;", "Lcom/fieldrecord/presentation/map/MapLayerStore;", "initial", "", "(Z)V", "value", "readSatellite", "writeSatellite", "", "app_debugUnitTest"})
    static final class MemoryStore implements com.fieldrecord.presentation.map.MapLayerStore {
        private boolean value;
        
        public MemoryStore(boolean initial) {
            super();
        }
        
        @java.lang.Override()
        public boolean readSatellite() {
            return false;
        }
        
        @java.lang.Override()
        public void writeSatellite(boolean value) {
        }
        
        public MemoryStore() {
            super();
        }
    }
}