package com.fieldrecord.presentation.gallery;

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
public final class GalleryViewModel_Factory implements Factory<GalleryViewModel> {
  private final Provider<FieldPhotoRepository> repositoryProvider;

  public GalleryViewModel_Factory(Provider<FieldPhotoRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GalleryViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static GalleryViewModel_Factory create(Provider<FieldPhotoRepository> repositoryProvider) {
    return new GalleryViewModel_Factory(repositoryProvider);
  }

  public static GalleryViewModel newInstance(FieldPhotoRepository repository) {
    return new GalleryViewModel(repository);
  }
}
