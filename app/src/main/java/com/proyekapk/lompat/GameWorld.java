package com.proyekapk.lompat;

import java.util.ArrayList;
import java.util.Random;

/**
 * Aturan main. Sengaja tanpa kode Android supaya bisa diuji di komputer biasa.
 *
 * Satuan: 360 satuan = tinggi area main. Posisi x dihitung dari tepi kiri layar,
 * tinggi dihitung dari tanah ke atas.
 */
final class GameWorld {

    static final int SIAP = 0;
    static final int MAIN = 1;
    static final int KALAH = 2;

    static final float TINGGI_LAYAR = 360f;
    /** Garis tanah, diukur dari atas area main. */
    static final float Y_TANAH = 296f;

    /** Kotak tabrak pemain, lebih ramping dari gambarnya supaya adil. */
    static final float LEBAR_PEMAIN = 30f;
    static final float TINGGI_PEMAIN = 76f;

    static final float GRAVITASI = 1750f;
    static final float LOMPAT = 640f;
    /** Lebih lemah dari lompatan pertama supaya pemain tidak keluar dari atas layar. */
    static final float LOMPAT_GANDA = 470f;
    static final float LAJU_AWAL = 250f;
    static final float LAJU_MAKS = 480f;
    /** Tambahan laju per detik. */
    static final float PERCEPATAN = 5f;
    /** Detik setelah kalah sebelum ketukan memulai ronde baru. */
    static final float JEDA_ULANG = 0.7f;
    /** Ketukan sesaat sebelum mendarat tetap dihitung sebagai lompatan. */
    static final float ANTRE_LOMPAT = 0.12f;

    static final int POT = 0;
    static final int PETI = 1;
    static final int POT_KEMBAR = 2;
    static final int TINGGI = 3;
    static final float[] LEBAR_RINTANGAN = {34f, 46f, 70f, 46f};
    static final float[] TINGGI_RINTANGAN = {44f, 46f, 44f, 76f};

    static final float UKURAN_BUNGA = 28f;
    static final int NILAI_BUNGA = 10;

    static final class Rintangan {
        int jenis;
        /** Tepi kiri. */
        float x;
    }

    static final class Bunga {
        /** Titik tengah. */
        float x;
        /** Tinggi titik tengah di atas tanah. */
        float tinggi;
    }

    final ArrayList<Rintangan> rintangan = new ArrayList<>();
    final ArrayList<Bunga> bunga = new ArrayList<>();
    private final Random acak;

    int keadaan = SIAP;
    float lebarLayar = 640f;
    /** Tepi kiri kotak tabrak pemain. */
    float xPemain = 70f;
    /** Tinggi telapak kaki di atas tanah. */
    float tinggiPemain;
    /** Positif berarti naik. */
    float kecepatanY;
    /** 0 = di tanah, 1 = sudah lompat, 2 = sudah lompat ganda. */
    int lompatanTerpakai;
    float laju = LAJU_AWAL;
    /** Jarak tempuh ronde ini. */
    float jarak;
    /** Pergeseran latar; terus bertambah, juga di layar pembuka. */
    float gulir;
    /** Detik sejak ronde dimulai. */
    float waktu;
    float waktuKalah;
    int bungaTerkumpul;
    int skor;
    int terbaik;
    boolean rekorBaru;

    /** Penanda kejadian untuk tampilan (getar, efek). Tampilan yang menghapusnya. */
    boolean baruKalah;
    boolean baruAmbilBunga;
    float xBungaTerakhir;
    float tinggiBungaTerakhir;

    private float jarakKeRintangan;
    private float antreLompat;

    GameWorld(long benih) {
        acak = new Random(benih);
    }

    void mulaiUlang() {
        rintangan.clear();
        bunga.clear();
        keadaan = MAIN;
        tinggiPemain = 0f;
        kecepatanY = 0f;
        lompatanTerpakai = 0;
        laju = LAJU_AWAL;
        jarak = 0f;
        waktu = 0f;
        waktuKalah = 0f;
        bungaTerkumpul = 0;
        skor = 0;
        rekorBaru = false;
        baruKalah = false;
        baruAmbilBunga = false;
        jarakKeRintangan = 120f;
        antreLompat = 0f;
    }

    /** Pemain mengetuk layar. */
    void ketuk() {
        if (keadaan == SIAP) {
            mulaiUlang();
        } else if (keadaan == KALAH) {
            if (waktuKalah >= JEDA_ULANG) {
                mulaiUlang();
            }
        } else if (lompatanTerpakai == 0) {
            lompat(LOMPAT);
        } else if (lompatanTerpakai == 1) {
            lompat(LOMPAT_GANDA);
        } else {
            antreLompat = ANTRE_LOMPAT;
        }
    }

    private void lompat(float kecepatan) {
        kecepatanY = kecepatan;
        lompatanTerpakai++;
    }

    /** Memajukan permainan sejauh dt detik. */
    void maju(float dt) {
        if (dt <= 0f) {
            return;
        }
        if (dt > 0.05f) {
            dt = 0.05f;
        }
        if (keadaan == SIAP) {
            gulir += 30f * dt;
            return;
        }
        if (keadaan == KALAH) {
            waktuKalah += dt;
            gerakkanPemain(dt, false);
            return;
        }

        waktu += dt;
        laju = Math.min(LAJU_MAKS, LAJU_AWAL + PERCEPATAN * waktu);
        float geser = laju * dt;
        jarak += geser;
        gulir += geser;

        gerakkanPemain(dt, true);
        if (antreLompat > 0f) {
            antreLompat -= dt;
        }

        for (int i = rintangan.size() - 1; i >= 0; i--) {
            Rintangan r = rintangan.get(i);
            r.x -= geser;
            if (r.x + LEBAR_RINTANGAN[r.jenis] < -60f) {
                rintangan.remove(i);
            }
        }
        for (int i = bunga.size() - 1; i >= 0; i--) {
            Bunga b = bunga.get(i);
            b.x -= geser;
            if (b.x < -60f) {
                bunga.remove(i);
            }
        }

        jarakKeRintangan -= geser;
        if (jarakKeRintangan <= 0f) {
            munculkanRintangan();
        }

        ambilBunga();
        skor = (int) (jarak / 12f) + bungaTerkumpul * NILAI_BUNGA;
        periksaTabrakan();
    }

    private void gerakkanPemain(float dt, boolean bolehAntre) {
        if (lompatanTerpakai == 0) {
            return;
        }
        kecepatanY -= GRAVITASI * dt;
        tinggiPemain += kecepatanY * dt;
        if (tinggiPemain <= 0f) {
            tinggiPemain = 0f;
            kecepatanY = 0f;
            lompatanTerpakai = 0;
            if (bolehAntre && antreLompat > 0f) {
                antreLompat = 0f;
                lompat(LOMPAT);
            }
        }
    }

    private void munculkanRintangan() {
        float p = acak.nextFloat();
        int jenis;
        if (waktu < 8f) {
            jenis = p < 0.6f ? POT : PETI;
        } else if (waktu < 22f) {
            jenis = p < 0.4f ? POT : p < 0.75f ? PETI : POT_KEMBAR;
        } else {
            jenis = p < 0.3f ? POT : p < 0.55f ? PETI : p < 0.8f ? POT_KEMBAR : TINGGI;
        }
        Rintangan r = new Rintangan();
        r.jenis = jenis;
        r.x = lebarLayar + 40f;
        rintangan.add(r);

        // Rintangan berikutnya datang 0,95 sampai 1,7 detik kemudian: selalu cukup
        // waktu untuk mendarat dan melompat lagi.
        float jedaDetik = 0.95f + acak.nextFloat() * 0.75f;
        float celah = laju * jedaDetik;
        jarakKeRintangan = LEBAR_RINTANGAN[jenis] + celah;

        float q = acak.nextFloat();
        if (q < 0.45f) {
            Bunga b = new Bunga();
            if (q < 0.25f) {
                // tepat di atas rintangan: terambil sambil melompatinya
                b.x = r.x + LEBAR_RINTANGAN[jenis] / 2f;
                b.tinggi = TINGGI_RINTANGAN[jenis] + 48f + acak.nextFloat() * 20f;
            } else {
                // di tengah celah, kadang perlu lompatan kecil
                b.x = r.x + LEBAR_RINTANGAN[jenis] + celah * 0.5f;
                b.tinggi = 60f + acak.nextFloat() * 70f;
            }
            bunga.add(b);
        }
    }

    private void ambilBunga() {
        float kiri = xPemain - 6f;
        float kanan = xPemain + LEBAR_PEMAIN + 6f;
        float bawah = tinggiPemain - 4f;
        float atas = tinggiPemain + TINGGI_PEMAIN + 6f;
        float jari = UKURAN_BUNGA / 2f;
        for (int i = bunga.size() - 1; i >= 0; i--) {
            Bunga b = bunga.get(i);
            if (b.x + jari > kiri && b.x - jari < kanan && b.tinggi + jari > bawah && b.tinggi - jari < atas) {
                bunga.remove(i);
                bungaTerkumpul++;
                baruAmbilBunga = true;
                xBungaTerakhir = b.x;
                tinggiBungaTerakhir = b.tinggi;
            }
        }
    }

    private void periksaTabrakan() {
        float kiri = xPemain;
        float kanan = xPemain + LEBAR_PEMAIN;
        for (int i = 0; i < rintangan.size(); i++) {
            Rintangan r = rintangan.get(i);
            // rintangan dihitung sedikit lebih kecil dari gambarnya
            float rKiri = r.x + 5f;
            float rKanan = r.x + LEBAR_RINTANGAN[r.jenis] - 5f;
            float rAtas = TINGGI_RINTANGAN[r.jenis] - 5f;
            if (kanan > rKiri && kiri < rKanan && tinggiPemain < rAtas) {
                kalah();
                return;
            }
        }
    }

    private void kalah() {
        keadaan = KALAH;
        waktuKalah = 0f;
        baruKalah = true;
        if (skor > terbaik) {
            terbaik = skor;
            rekorBaru = true;
        }
        // terpental kecil lalu jatuh
        kecepatanY = 260f;
        lompatanTerpakai = 2;
        antreLompat = 0f;
    }
}
