# Lompat-Lompat

Game Android sederhana: tokoh bergaun putih berlari membawa buket bunga, dan kamu mengetuk layar untuk melompati pot dan peti. Kodenya ditulis Claude, lalu dirakit otomatis oleh GitHub Actions.

## Unduh APK

[Unduh versi terbaru](https://github.com/satyapermanayoga-code/aplikasi-androiz/releases/latest/download/lompat-lompat.apk)

Buka tautan itu di HP Android, lalu ketuk file yang terunduh untuk memasangnya. Android akan meminta izin memasang aplikasi dari browser satu kali.

## Cara main

- Ketuk layar untuk mulai, lalu ketuk untuk melompat.
- Ketuk sekali lagi di udara untuk lompat ganda.
- Bunga bernilai 10 poin. Skor terbaik disimpan di HP.

## Cara kerjanya

1. Kode baru masuk ke repo ini.
2. Workflow `.github/workflows/rakit-apk.yml` merakit APK di server GitHub.
3. APK diterbitkan di halaman Releases.

## Isi repo

- `app/`: kode game (Java, tanpa pustaka tambahan)
  - `GameWorld.java`: aturan main (lompat, rintangan, skor)
  - `GameView.java`: gambar dan sentuhan
  - `MainActivity.java`: layar penuh mendatar
- `alat/buat_aset.py`: menggambar avatar, rintangan, dan ikon dari kode. Tidak ada foto asli di repo ini.
- `alat/pratinjau/`: alat uji di komputer, untuk menjalankan aturan main dan merender contoh layar tanpa HP
- `debug.keystore`: kunci tanda tangan uji coba, supaya versi baru bisa dipasang menimpa versi lama. Bukan untuk rilis ke Play Store.
- `gradlew`, `gradle/`: Gradle wrapper (Gradle 8.14.3, Android Gradle Plugin 8.13.2)
