# FieldRecord (AgriOps Mobile)

**FieldRecord** is a high-precision, offline-first mobile application built with **Flutter**, designed specifically for estate operations, Smart Grading (TPH standardization), and field data collection in palm oil plantations (SBYE Rayon 1 / Askep Rayon 1 standards).

## 🚀 Key Features

- **Offline-First Data Collection:** Built with Drift (SQLite) and Riverpod state management, ensuring seamless data capture without internet connectivity in remote estate blocks.
- **High-Precision GPS & OSMDroid:** Custom map layer toggles, North-lock, accurate accuracy circle scaling (`setDrawAccuracyEnabled`), and red dot pins for photo coordinates.
- **Specialized Camera UI:**
  - Locked 4:3 viewfinder preview centered.
  - GPS status and telemetry positioned at bottom-left, compass heading & cardinal/degrees at bottom-right (for both portrait and landscape rotation).
  - Watermark bar (80px) and zoom controls (0.5x, 1x, 2x with hardware detection).
  - Monthly-reset naming format: `IMG_mmxxxx.jpg` / `IMG_YYMMdd_xxx.jpg` saved to `/storage/emulated/0/FieldRecord/Photo`.
- **Gallery & Clustering:** 1:1 square grid, fullscreen swipe viewer, and screen-distance photo clustering with count badges.
- **Cross-Department Architecture:** Seamless integration with backend SaaS (FastAPI, PostgreSQL, PKD / EstatePlan.io APIs) and multi-agent coordination (`mobile-dev` and `saas-dev`).

## 🛠 Tech Stack

- **Framework:** Flutter (Dart)
- **State Management:** Riverpod
- **Local Storage:** Drift (SQLite)
- **Maps:** OSMDroid with custom GPS overlays
- **Deployment:** Wireless APK deployment via Tailscale / USB

## 📦 Build & Release

```bash
flutter pub get
flutter build apk --release
```
