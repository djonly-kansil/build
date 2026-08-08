# App Controll

Aplikasi Android native (Kotlin + Jetpack Compose) untuk memantau RAM dan mengelola aplikasi
tanpa root maupun Shizuku. Penghentian aplikasi, pembersihan cache, dan uninstall dijalankan
otomatis melalui `AccessibilityService` yang menekan tombol di layar Setelan sistem, dibungkus
foreground service + overlay progres agar layar tidak berkedip.

- Package: `com.taloarane.appcontroll`
- minSdk 24 · targetSdk/compileSdk 36
- Kotlin 2.2 · Compose Material 3 · DataStore · Coil

## Struktur

```text
app/src/main/java/com/taloarane/appcontroll
├─ MainActivity.kt          layar utama (RAM, bersihkan, filter, tab, daftar app, bottom nav)
├─ data/                    RamRepository, AppRepository, StorageRepository, Prefs (DataStore)
├─ service/                 CleanQueue, AppControllAccessibilityService, CleanForegroundService
└─ ui/                      tema gelap/terang, string ID/EN, ViewModel, komponen & layar
```

## Build lokal (Termux)

```bash
# sekali saja: buat Gradle wrapper
gradle wrapper --gradle-version 8.13

./gradlew assembleDebug
```

## Build di GitHub Actions

Workflow `.github/workflows/build.yml` memakai JDK 17 (temurin), membuat wrapper otomatis bila
`gradlew` belum ada, lalu menjalankan `./gradlew assembleRelease` dan mengunggah APK sebagai artifact.

Signing memakai repository secrets:

| Secret | Isi |
| --- | --- |
| `KEYSTORE` | keystore `.jks` dalam base64 |
| `KEYSTORE_PASSWORD` | password keystore |
| `ALIAS` | nama alias kunci |
| `ALIAS_PASSWORD` | password alias |

Bila `KEYSTORE` kosong, build tetap jalan dan menghasilkan APK release tanpa tanda tangan.

## Izin yang diminta saat onboarding

Aksesibilitas, PACKAGE_USAGE_STATS, MANAGE_EXTERNAL_STORAGE, POST_NOTIFICATIONS,
SYSTEM_ALERT_WINDOW (overlay), QUERY_ALL_PACKAGES, REQUEST_DELETE_PACKAGES.
