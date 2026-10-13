package com.fieldrecord.presentation.map;

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
public final class MapViewModel_Factory implements Factory<MapViewModel> {
  private final Provider<FieldPhotoRepository> repositoryProvider;

  public MapViewModel_Factory(Provider<FieldPhotoRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public MapViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static MapViewModel_Factory create(Provider<FieldPhotoRepository> repositoryProvider) {
    return new MapViewModel_Factory(repositoryProvider);
  }

  public static MapViewModel newInstance(FieldPhotoRepository repository) {
    return new MapViewModel(repository);
  }
}
