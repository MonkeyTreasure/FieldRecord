# FieldRecord (AgriOps Mobile)

**FieldRecord** adalah aplikasi mobile berbasis **Kotlin (Android)** dengan arsitektur modern (MVVM + Clean Architecture + Hilt), dirancang khusus untuk operasi perkebunan kelapa sawit, Smart Grading (standarisasi TPH per Oktober 2026), serta pencatatan lapangan berpresisi tinggi (Standar SBYE Rayon 1 / Askep Rayon 1).

## 🚀 Fitur Utama

- **Offline-First Storage:** Penyimpanan lokal menggunakan Room Database / SQLite, aman dioperasikan di blok kebun tanpa sinyal internet.
- **High-Precision GPS & Kalman Filtering:** Pelacakan koordinat presisi tinggi menggunakan filter Kalman (`LocationKalmanFilter` & `LocationTrackingService`), penanda pin titik koordinat merah di peta (`OSMDroid`).
- **Specialized Camera & Watermark UI:**
  - Tampilan kamera terkunci pada rasio 4:3 (portrait/landscape).
  - Watermark otomatis di bar bawah foto (80px) berisi ID Unit/Komplek, tanggal, koordinat GPS (lat, lon), arah kompas (bearing & cardinal), serta warna teks/latar dapat dikustomisasi.
  - Reset nomor urut bulanan dengan format penamaan file: `IMG_YYMMdd_xxx.jpg` atau `IMG_mmxxxx.jpg` yang disimpan di direktori publik `/storage/emulated/0/FieldRecord/Photo`.
- **WhatsApp Share & Pinpoint Map:**
  - Fitur berbagi foto langsung ke **WhatsApp** beserta catatan lapangan dan tautan pinpoint lokasi Google Maps (`https://maps.google.com/?q=lat,lon`) menggunakan `Intent.ACTION_SEND` dan `FileProvider`.
- **Gallery & Viewer:**
  - Grid galeri foto 1:1, penampil layar penuh (*fullscreen swipe viewer*), klasterisasi foto berdasarkan jarak layar dengan badge jumlah foto.

## 🛠 Tech Stack

- **Language:** Kotlin
- **Architecture:** MVVM, Clean Architecture
- **DI:** Dagger Hilt
- **Local DB:** Room / SQLite
- **Map / Location:** OSMDroid, Fused Location Provider, Kalman Filter
- **Sharing:** Android Intent (`ACTION_SEND`, `FileProvider`)

## 📦 Build & Release

```bash
# Clone & build debug/release APK
./gradlew assembleDebug
```
