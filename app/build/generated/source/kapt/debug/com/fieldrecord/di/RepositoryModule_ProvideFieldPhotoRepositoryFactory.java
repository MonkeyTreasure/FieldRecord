package com.fieldrecord.di;

import com.fieldrecord.data.repository.FieldPhotoRepositoryImpl;
import com.fieldrecord.domain.repository.FieldPhotoRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class RepositoryModule_ProvideFieldPhotoRepositoryFactory implements Factory<FieldPhotoRepository> {
  private final Provider<FieldPhotoRepositoryImpl> implProvider;

  public RepositoryModule_ProvideFieldPhotoRepositoryFactory(
      Provider<FieldPhotoRepositoryImpl> implProvider) {
    this.implProvider = implProvider;
  }

  @Override
  public FieldPhotoRepository get() {
    return provideFieldPhotoRepository(implProvider.get());
  }

  public static RepositoryModule_ProvideFieldPhotoRepositoryFactory create(
      Provider<FieldPhotoRepositoryImpl> implProvider) {
    return new RepositoryModule_ProvideFieldPhotoRepositoryFactory(implProvider);
  }

  public static FieldPhotoRepository provideFieldPhotoRepository(FieldPhotoRepositoryImpl impl) {
    return Preconditions.checkNotNullFromProvides(RepositoryModule.INSTANCE.provideFieldPhotoRepository(impl));
  }
}
