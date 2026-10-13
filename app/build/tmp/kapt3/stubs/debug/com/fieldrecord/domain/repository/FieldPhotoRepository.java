package com.fieldrecord.domain.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a6@\u00a2\u0006\u0002\u0010\u0006J\u0014\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\t0\bH&J$\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00050\t2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\fH\u00a6@\u00a2\u0006\u0002\u0010\u000eJ\u0014\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00050\tH\u00a6@\u00a2\u0006\u0002\u0010\u0010J\u0016\u0010\u0011\u001a\u00020\f2\u0006\u0010\u0004\u001a\u00020\u0005H\u00a6@\u00a2\u0006\u0002\u0010\u0006J$\u0010\u0012\u001a\u00020\u00032\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00140\t2\u0006\u0010\u0015\u001a\u00020\u0016H\u00a6@\u00a2\u0006\u0002\u0010\u0017J \u0010\u0018\u001a\u00020\u00032\u0006\u0010\u0019\u001a\u00020\f2\b\u0010\u001a\u001a\u0004\u0018\u00010\u001bH\u00a6@\u00a2\u0006\u0002\u0010\u001c\u00a8\u0006\u001d"}, d2 = {"Lcom/fieldrecord/domain/repository/FieldPhotoRepository;", "", "deletePhoto", "", "photo", "Lcom/fieldrecord/domain/model/FieldPhoto;", "(Lcom/fieldrecord/domain/model/FieldPhoto;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllPhotos", "Lkotlinx/coroutines/flow/Flow;", "", "getPhotosByDateRange", "start", "", "end", "(JJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getPhotosWithNotes", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insertPhoto", "syncPhotos", "files", "Ljava/io/File;", "context", "Landroid/content/Context;", "(Ljava/util/List;Landroid/content/Context;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updatePhotoNote", "photoId", "note", "", "(JLjava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface FieldPhotoRepository {
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.fieldrecord.domain.model.FieldPhoto>> getAllPhotos();
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getPhotosByDateRange(long start, long end, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.fieldrecord.domain.model.FieldPhoto>> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getPhotosWithNotes(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.fieldrecord.domain.model.FieldPhoto>> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object syncPhotos(@org.jetbrains.annotations.NotNull()
    java.util.List<? extends java.io.File> files, @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updatePhotoNote(long photoId, @org.jetbrains.annotations.Nullable()
    java.lang.String note, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertPhoto(@org.jetbrains.annotations.NotNull()
    com.fieldrecord.domain.model.FieldPhoto photo, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Long> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deletePhoto(@org.jetbrains.annotations.NotNull()
    com.fieldrecord.domain.model.FieldPhoto photo, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}