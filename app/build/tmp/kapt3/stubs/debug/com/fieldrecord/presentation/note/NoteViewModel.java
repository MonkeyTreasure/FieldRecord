package com.fieldrecord.presentation.note;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\r\u001a\u00020\u000eJ\u0018\u0010\u000f\u001a\u00020\u000e2\u0006\u0010\u0010\u001a\u00020\u00112\b\u0010\u0012\u001a\u0004\u0018\u00010\u0013R\u001a\u0010\u0005\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lcom/fieldrecord/presentation/note/NoteViewModel;", "Landroidx/lifecycle/ViewModel;", "repo", "Lcom/fieldrecord/domain/repository/FieldPhotoRepository;", "(Lcom/fieldrecord/domain/repository/FieldPhotoRepository;)V", "_notedPhotos", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "Lcom/fieldrecord/domain/model/FieldPhoto;", "notedPhotos", "Lkotlinx/coroutines/flow/StateFlow;", "getNotedPhotos", "()Lkotlinx/coroutines/flow/StateFlow;", "refresh", "", "updateNote", "photoId", "", "note", "", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class NoteViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.fieldrecord.domain.repository.FieldPhotoRepository repo = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.fieldrecord.domain.model.FieldPhoto>> _notedPhotos = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.fieldrecord.domain.model.FieldPhoto>> notedPhotos = null;
    
    @javax.inject.Inject()
    public NoteViewModel(@org.jetbrains.annotations.NotNull()
    com.fieldrecord.domain.repository.FieldPhotoRepository repo) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.fieldrecord.domain.model.FieldPhoto>> getNotedPhotos() {
        return null;
    }
    
    public final void refresh() {
    }
    
    public final void updateNote(long photoId, @org.jetbrains.annotations.Nullable()
    java.lang.String note) {
    }
}