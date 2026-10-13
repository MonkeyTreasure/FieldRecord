# PRD: Field Record
**Versi:** 1.0  
**Tanggal:** 2026-09-23  
**Author:** Novianto Syahmud  
**Platform:** Android (Kotlin + Jetpack Compose)  
**Arsitektur:** MVVM + Clean Architecture (Repository pattern)  
**Min SDK:** 24 (Android 7.0)  
**Target SDK:** 34  

---

## 1. Ringkasan Produk
Aplikasi **Field Record** digunakan untuk mendokumentasikan foto lapangan dengan konteks geografis penuh. Setiap foto yang diambil/dipilih akan otomatis:
- Terekam koordinat GPS (lat/lng) + bearing (arah mata angin)
- Tersimpan di **internal storage** folder `FieldRecords/`
- **Penamaan file: `IMG_mmxxxx.jpg`**  
  `mm` = bulan 2-digit (01-12), `xxxx` = urut 4-digit **reset setiap awal bulan** (0001, 0002, …).  
- **Rasio foto kamera:** **4:3 (landscape)** / **3:4 (portrait)** — di-crop otomatis sebelum disimpan.  
- Ditampilkan sebagai pin di peta (Map) dengan clustering
- Bisa dibuka galeri penuh dengan metadata lengkap

---

## 2. User Flow (Diperbaiki)

```mermaid
flowchart TD
    A[Buka App] --> B{Izin Lokasi & Storage?}
    B -- Tidak --> C[Minta Izin Runtime]
    B -- Ya --> D[Home: Peta + FAB Kamera]
    D --> E[Tekan FAB Kamera]
    E --> F[Buka Kamera (CameraX)]
    F --> G[Ambil Foto / Pilih dari Galeri]
    G --> H[Ekstrak EXIF GPS + Compass Sensor]
    H --> I[Jika tidak ada GPS → gunakan last known location]
    I --> J[Crop otomatis 4:3 (L) / 3:4 (P)]
    J --> K[Simpan File ke FieldRecords/IMG_mmxxxx.jpg]
    K --> L[Tulis metadata ke Room DB]
    L --> M[Update Marker di Peta]
    M --> N[Toast: "Foto tersimpan & dipin"]
    N --> D
    
    D --> O[Tap Marker di Peta]
    O --> P[Bottom Sheet: Thumbnail + Metadata]
    P --> Q[Tap Thumbnail → Fullscreen Viewer]
    Q --> R[Swipe kiri/kanan navigate foto]
    
    D --> S[Tab Galeri]
    S --> T[Grid 3-kolom 1:1 + Header Tanggal]
    T --> Q
```

---

## 3. Fitur Utama & Acceptance Criteria

| ID | Fitur | Acceptance Criteria |
|----|-------|---------------------|
| FR-01 | **Kamera & Capture** | CameraX `ImageCapture` output `File` ke `FieldRecords/`; preview 4:3 landscape / 3:4 portrait; **crop otomatis ke rasio 4:3 (landscape) atau 3:4 (portrait) sebelum simpan**; watermark opsional (GPS + bearing). Penamaan **`IMG_mmxxxx.jpg`** (reset bulanan). |
| FR-02 | **Geo-tagging Otomatis** | Setiap foto wajib memiliki `lat`, `lng`, `bearing`, `timestamp`. Jika GPS null → fallback ke `FusedLocationProviderClient.lastLocation`. |
| FR-03 | **Internal Storage** | Folder `Environment.getExternalStorageDirectory()/FieldRecords/` (atau `context.filesDir/FieldRecords` untuk scoped storage Android 10+). **File naming `IMG_mmxxxx.jpg`** (reset bulanan). |
| FR-04 | **Database (Room)** | Entity `FieldPhoto(id, path, lat, lng, bearing, timestamp, note?)`. Index pada `timestamp` & `lat/lng`. |
| FR-05 | **Peta (OSMDroid / OSM Raster)** | Tampilkan marker merah cluster; tap marker → bottom sheet preview; long-press → hapus foto (konfirmasi). |
| FR-06 | **Galeri** | Grid 3 kolom, aspect 1:1, sticky header per tanggal (`YYYY / dd MMM`), lazy load (Paging 3). Tap → fullscreen `HorizontalPager` dengan zoom pinch. |
| FR-07 | **Offline-first** | Semua data lokal; tidak ada network call wajib. Export/Import JSON opsional nanti. |
| FR-08 | **Izin Runtime** | `ACCESS_FINE_LOCATION`, `CAMERA`, `READ_MEDIA_IMAGES` (Android 13+). |

---

## 4. Teknologi & Dependencies (build.gradle.kts)

```kotlin
// Versi
val composeBom = "2024.06.00"
val roomVersion = "2.6.1"
val cameraXVersion = "1.4.0"
val maplibreVersion = "11.5.0"
val pagingVersion = "3.2.1"
val hiltVersion = "2.48"

// Plugins
id("com.android.application")
id("org.jetbrains.kotlin.android")
id("com.google.dagger.hilt.android")
id("kotlin-kapt")

// Dependencies
implementation(platform("androidx.compose:compose-bom:$composeBom"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.activity:activity-compose:1.9.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
implementation("androidx.navigation:navigation-compose:2.7.7")

// CameraX
implementation("androidx.camera:camera-core:${cameraXVersion}")
implementation("androidx.camera:camera-camera2:${cameraXVersion}")
implementation("androidx.camera:camera-view:${cameraXVersion}")
implementation("androidx.camera:camera-video:${cameraXVersion}")

// Room
implementation("androidx.room:room-runtime:${roomVersion}")
implementation("androidx.room:room-ktx:${roomVersion}")
kapt("androidx.room:room-compiler:${roomVersion}")

// OSMDroid
implementation("org.osmdroid:osmdroid-android:6.1.18")

// Paging
implementation("androidx.paging:paging-compose:${pagingVersion}")

// Hilt
implementation("com.google.dagger:hilt-android:${hiltVersion}")
kapt("com.google.dagger:hilt-compiler:${hiltVersion}")

// Coroutines / Flow
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
```

---

## 5. Struktur Package (Clean + MVVM)

```
com.fieldrecord
├── data
│   ├── local
│   │   ├── dao/FieldPhotoDao.kt
│   │   ├── db/FieldRecordDatabase.kt
│   │   └── entity/FieldPhoto.kt
│   └── repository/FieldPhotoRepositoryImpl.kt
├── domain
│   ├── model/FieldPhoto.kt
│   ├── repository/FieldPhotoRepository.kt
│   └── usecase/
│       ├── CapturePhotoUseCase.kt
│       ├── GetPhotosUseCase.kt
│       └── DeletePhotoUseCase.kt
├── presentation
│   ├── map/MapScreen.kt, MapViewModel.kt
│   ├── gallery/GalleryScreen.kt, GalleryViewModel.kt
│   ├── camera/CameraScreen.kt, CameraViewModel.kt
│   ├── viewer/FullscreenViewerScreen.kt
│   └── common/MapMarkerCluster.kt
├── di/ AppModule.kt, DatabaseModule.kt
└── MainActivity.kt, FieldRecordApplication.kt
```

---

## 6. Alur Data (Single Source of Truth)

1. **CapturePhotoUseCase**  
   - Kamera → `File` + `Location` + `Bearing` → `FieldPhoto` entity → `Repository.insert()` → `Flow<List<FieldPhoto>>` update UI.
2. **MapScreen** observe `photosFlow` → render markers via `MapLibre` `SymbolLayer` + clustering.
3. **GalleryScreen** `PagingSource` dari Room `timestamp DESC` → `LazyVerticalGrid`.
4. **FullscreenViewer** terima `List<FieldPhoto>` + `initialIndex` → `HorizontalPager`.

---

## 7. UI / UX Guidelines

- **Tema**: Material 3, Dark default, Accent **Emerald #2E7D32** / **Amber #FFB300** (mirip AgriOps).  
- **Tipografi**: Inter (Google Fonts).  
- **FAB Kamera** di tengah-bottom, elevated 8dp, background Emerald, icon white.  
- **Bottom Sheet** marker: tinggi 40% layar, drag handle, thumbnail 1:1, metadata 2 baris (Date / Longlat / Bearing).  
- **Galeri**: Header sticky `Text("2026\n23/09", style=labelLarge, color=amber)`. Grid spacing 4dp.  
- **Aksesibilitas**: contentDescription pada semua gambar, touch target ≥ 48dp.

---

## 8. Non-Functional

| Kategori | Target |
|----------|--------|
| Cold start | < 1.5 s (pada Snapdragon 660) |
| APK size | < 25 MB (Release, R8 full) |
| Battery | Location request interval ≥ 5 s saat idle |
| Crash-free | ≥ 99.5 % (Play Console) |

---

## 9. Rencana Iterasi (Sprint)

| Sprint | Deliverable |
|--------|-------------|
| 1 | Project setup, Hilt, Room, CameraX capture + simpan file + DB insert. |
| 2 | OSMDroid integration, marker clustering, bottom sheet preview. |
| 3 | Gallery (Paging 3 grid + sticky header), Fullscreen viewer swipe + pinch zoom. |
| 4 | Polish: watermark, EXIF write, izin runtime flow, export JSON, unit/UI test. |

---

## 10. Risiko & Mitigasi

| Risiko | Mitigasi |
|--------|----------|
| Scoped Storage Android 10+ | Gunakan `MediaStore` untuk folder `FieldRecords` di `Pictures/` agar user visible, atau `context.filesDir` untuk privat. |
| GPS tidak akurat indoor | Fallback ke last known + tampilkan indikator akurasi di metadata. |
| OSMDroid tile caching | Pastikan cache tile di-set dengan benar untuk mode offline. |

---

## 11. Definition of Done (per Sprint)

- Semua AC di atas terpenuhi (manual + Espresso test).  
- `./gradlew lintDebug` zero error.  
- `./gradlew testDebugUnitTest` 100% pass.  
- Build release `assembleRelease` berhasil & di-install di device fisik.  
- Dokumentasi README + diagram arsitektur ter-update.

---

*Catatan: PRD ini *living document* – update setiap sprint review.*