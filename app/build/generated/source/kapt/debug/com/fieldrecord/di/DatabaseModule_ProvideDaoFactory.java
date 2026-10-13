package com.fieldrecord.di;

import com.fieldrecord.data.local.dao.FieldPhotoDao;
import com.fieldrecord.data.local.db.FieldRecordDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideDaoFactory implements Factory<FieldPhotoDao> {
  private final Provider<FieldRecordDatabase> databaseProvider;

  public DatabaseModule_ProvideDaoFactory(Provider<FieldRecordDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public FieldPhotoDao get() {
    return provideDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideDaoFactory create(
      Provider<FieldRecordDatabase> databaseProvider) {
    return new DatabaseModule_ProvideDaoFactory(databaseProvider);
  }

  public static FieldPhotoDao provideDao(FieldRecordDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDao(database));
  }
}
