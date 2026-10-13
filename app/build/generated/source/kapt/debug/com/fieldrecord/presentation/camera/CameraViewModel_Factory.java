package com.fieldrecord.presentation.camera;

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
public final class CameraViewModel_Factory implements Factory<CameraViewModel> {
  private final Provider<FieldPhotoRepository> repositoryProvider;

  public CameraViewModel_Factory(Provider<FieldPhotoRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public CameraViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static CameraViewModel_Factory create(Provider<FieldPhotoRepository> repositoryProvider) {
    return new CameraViewModel_Factory(repositoryProvider);
  }

  public static CameraViewModel newInstance(FieldPhotoRepository repository) {
    return new CameraViewModel(repository);
  }
}
