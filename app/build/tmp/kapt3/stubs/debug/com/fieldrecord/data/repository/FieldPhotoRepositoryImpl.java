package com.fieldrecord.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0096@\u00a2\u0006\u0002\u0010\tJ\u0014\u0010\n\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\f0\u000bH\u0016J$\u0010\r\u001a\b\u0012\u0004\u0012\u00020\b0\f2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u000fH\u0096@\u00a2\u0006\u0002\u0010\u0011J\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\b0\fH\u0096@\u00a2\u0006\u0002\u0010\u0013J\u0016\u0010\u0014\u001a\u00020\u000f2\u0006\u0010\u0007\u001a\u00020\bH\u0096@\u00a2\u0006\u0002\u0010\tJ$\u0010\u0015\u001a\u00020\u00062\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00170\f2\u0006\u0010\u0018\u001a\u00020\u0019H\u0096@\u00a2\u0006\u0002\u0010\u001aJ \u0010\u001b\u001a\u00020\u00062\u0006\u0010\u001c\u001a\u00020\u000f2\b\u0010\u001d\u001a\u0004\u0018\u00010\u001eH\u0096@\u00a2\u0006\u0002\u0010\u001fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006 "}, d2 = {"Lcom/fieldrecord/data/repository/FieldPhotoRepositoryImpl;", "Lcom/fieldrecord/domain/repository/FieldPhotoRepository;", "dao", "Lcom/fieldrecord/data/local/dao/FieldPhotoDao;", "(Lcom/fieldrecord/data/local/dao/FieldPhotoDao;)V", "deletePhoto", "", "photo", "Lcom/fieldrecord/domain/model/FieldPhoto;", "(Lcom/fieldrecord/domain/model/FieldPhoto;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllPhotos", "Lkotlinx/coroutines/flow/Flow;", "", "getPhotosByDateRange", "start", "", "end", "(JJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getPhotosWithNotes", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insertPhoto", "syncPhotos", "files", "Ljava/io/File;", "context", "Landroid/content/Context;", "(Ljava/util/List;Landroid/content/Context;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updatePhotoNote", "photoId", "note", "", "(JLjava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class FieldPhotoRepositoryImpl implements com.fieldrecord.domain.repository.FieldPhotoRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.fieldrecord.data.local.dao.FieldPhotoDao dao = null;
    
    @javax.inject.Inject()
    public FieldPhotoRepositoryImpl(@org.jetbrains.annotations.NotNull()
    com.fieldrecord.data.local.dao.FieldPhotoDao dao) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<com.fieldrecord.domain.model.FieldPhoto>> getAllPhotos() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getPhotosByDateRange(long start, long end, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.fieldrecord.domain.model.FieldPhoto>> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object getPhotosWithNotes(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.fieldrecord.domain.model.FieldPhoto>> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object syncPhotos(@org.jetbrains.annotations.NotNull()
    java.util.List<? extends java.io.File> files, @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object updatePhotoNote(long photoId, @org.jetbrains.annotations.Nullable()
    java.lang.String note, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object insertPhoto(@org.jetbrains.annotations.NotNull()
    com.fieldrecord.domain.model.FieldPhoto photo, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object deletePhoto(@org.jetbrains.annotations.NotNull()
    com.fieldrecord.domain.model.FieldPhoto photo, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}