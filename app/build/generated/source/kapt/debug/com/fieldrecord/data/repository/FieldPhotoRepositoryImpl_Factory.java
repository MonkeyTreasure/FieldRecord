package com.fieldrecord.data.repository;

import com.fieldrecord.data.local.dao.FieldPhotoDao;
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
public final class FieldPhotoRepositoryImpl_Factory implements Factory<FieldPhotoRepositoryImpl> {
  private final Provider<FieldPhotoDao> daoProvider;

  public FieldPhotoRepositoryImpl_Factory(Provider<FieldPhotoDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public FieldPhotoRepositoryImpl get() {
    return newInstance(daoProvider.get());
  }

  public static FieldPhotoRepositoryImpl_Factory create(Provider<FieldPhotoDao> daoProvider) {
    return new FieldPhotoRepositoryImpl_Factory(daoProvider);
  }

  public static FieldPhotoRepositoryImpl newInstance(FieldPhotoDao dao) {
    return new FieldPhotoRepositoryImpl(dao);
  }
}
