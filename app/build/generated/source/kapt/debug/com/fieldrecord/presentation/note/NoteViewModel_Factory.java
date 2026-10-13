package com.fieldrecord.presentation.note;

import com.fieldrecord.domain.repository.FieldPhotoRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class NoteViewModel_Factory implements Factory<NoteViewModel> {
  private final Provider<FieldPhotoRepository> repoProvider;

  public NoteViewModel_Factory(Provider<FieldPhotoRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public NoteViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static NoteViewModel_Factory create(Provider<FieldPhotoRepository> repoProvider) {
    return new NoteViewModel_Factory(repoProvider);
  }

  public static NoteViewModel newInstance(FieldPhotoRepository repo) {
    return new NoteViewModel(repo);
  }
}
