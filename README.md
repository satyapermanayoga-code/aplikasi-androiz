# Halo Dunia

Aplikasi Android percobaan untuk menguji jalur perakitan APK: kodenya ditulis Claude, lalu dirakit otomatis oleh GitHub Actions.

## Unduh APK

[Unduh versi terbaru](https://github.com/satyapermanayoga-code/aplikasi-androiz/releases/latest/download/halo-dunia.apk)

Buka tautan itu di HP Android, lalu ketuk file yang terunduh untuk memasangnya. Android akan meminta izin memasang aplikasi dari browser satu kali.

## Cara kerjanya

1. Kode baru masuk ke repo ini.
2. Workflow `.github/workflows/rakit-apk.yml` merakit APK di server GitHub.
3. APK diterbitkan di halaman Releases.

## Isi repo

- `app/`: kode aplikasi (satu layar, Java, tanpa pustaka tambahan)
- `debug.keystore`: kunci tanda tangan uji coba, supaya versi baru bisa dipasang menimpa versi lama. Bukan untuk rilis ke Play Store.
- `gradlew`, `gradle/`: Gradle wrapper (Gradle 8.14.3, Android Gradle Plugin 8.13.2)
