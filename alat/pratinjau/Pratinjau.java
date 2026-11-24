package com.proyekapk.lompat;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Random;

/**
 * Uji aturan main dan render contoh layar di komputer, tanpa HP.
 * Dijalankan lewat alat/pratinjau/jalankan.sh dengan kelas Android tiruan.
 */
public final class Pratinjau {

    private static final float DT = 1f / 60f;
    private static final int LAYAR_SIAP = 0;
    private static final int LAYAR_MAIN = 1;
    private static final int LAYAR_KALAH = 2;
    private static final int LAYAR_JEDA = 3;

    public static void main(String[] argumen) throws Exception {
        String keluaran = argumen.length > 0 ? argumen[0] : ".";
        ujiLompatan();
        ujiTanpaKetukan();
        ujiJedaUlang();
        ujiSentuhanDanJeda();
        cetakKelonggaran();
        ujiBotSempurna();
        ujiBotCeroboh();
        renderContoh(keluaran);
        System.out.println("SEMUA UJI LULUS");
    }

    private static void pastikan(boolean syarat, String pesan) {
        if (!syarat) {
            throw new AssertionError(pesan);
        }
    }

    // ------------------------------------------------------------ aturan

    private static void ujiLompatan() {
        GameWorld d = new GameWorld(1);
        d.ketuk();
        d.ketuk();
        float puncak = 0f;
        float lama = 0f;
        while (d.lompatanTerpakai > 0) {
            d.maju(DT);
            lama += DT;
            puncak = Math.max(puncak, d.tinggiPemain);
        }
        System.out.printf(Locale.ROOT, "lompat biasa: puncak %.0f satuan, di udara %.2f detik%n", puncak, lama);
        pastikan(puncak > 105f && puncak < 125f, "tinggi lompatan di luar perkiraan");
        pastikan(lama > 0.6f && lama < 0.85f, "lama lompatan di luar perkiraan");

        d = new GameWorld(1);
        d.ketuk();
        d.ketuk();
        puncak = 0f;
        int ketukanDiUdara = 0;
        while (d.lompatanTerpakai > 0) {
            d.maju(DT);
            if (ketukanDiUdara == 0 && d.kecepatanY <= 0f) {
                d.ketuk();
                ketukanDiUdara++;
            }
            puncak = Math.max(puncak, d.tinggiPemain);
        }
        System.out.printf(Locale.ROOT, "lompat ganda: puncak %.0f satuan%n", puncak);
        pastikan(puncak > 160f && puncak < 185f, "tinggi lompat ganda di luar perkiraan");
        // gambar avatar setinggi 118 satuan di atas telapak kaki: harus tetap di dalam layar
        pastikan(GameWorld.Y_TANAH - puncak - 118f >= 0f, "lompat ganda membuat avatar keluar dari atas layar");

        // ketukan ketiga di udara tidak boleh menambah lompatan
        d = new GameWorld(1);
        d.ketuk();
        d.ketuk();
        d.maju(DT);
        d.ketuk();
        d.maju(DT);
        float sebelum = d.kecepatanY;
        d.ketuk();
        pastikan(d.kecepatanY == sebelum && d.lompatanTerpakai == 2, "lompatan ketiga seharusnya ditolak");
    }

    private static void ujiTanpaKetukan() {
        GameWorld d = new GameWorld(7);
        d.lebarLayar = 780f;
        d.ketuk();
        float t = 0f;
        while (d.keadaan == GameWorld.MAIN && t < 10f) {
            d.maju(DT);
            t += DT;
        }
        System.out.printf(Locale.ROOT, "tanpa ketukan: kalah setelah %.1f detik, skor %d%n", t, d.skor);
        pastikan(d.keadaan == GameWorld.KALAH, "tanpa melompat seharusnya menabrak");
        pastikan(t > 2f, "rintangan pertama datang terlalu cepat");
    }

    private static void ujiJedaUlang() {
        GameWorld d = new GameWorld(3);
        d.ketuk();
        while (d.keadaan == GameWorld.MAIN) {
            d.maju(DT);
        }
        int skorAkhir = d.skor;
        pastikan(d.terbaik == skorAkhir && d.rekorBaru, "rekor pertama seharusnya tercatat");
        d.ketuk();
        pastikan(d.keadaan == GameWorld.KALAH, "ketukan terlalu cepat tidak boleh memulai ulang");
        for (int i = 0; i < 60; i++) {
            d.maju(DT);
        }
        pastikan(d.tinggiPemain == 0f, "pemain seharusnya sudah jatuh ke tanah");
        d.ketuk();
        pastikan(d.keadaan == GameWorld.MAIN && d.skor == 0 && d.rintangan.isEmpty(), "ronde baru harus bersih");
        pastikan(d.terbaik == skorAkhir, "skor terbaik harus bertahan");
    }

    private static void ujiSentuhanDanJeda() throws InterruptedException {
        Context konteks = new Context();
        GameView v = new GameView(konteks);
        v.onSizeChanged(2400, 1080, 0, 0);
        pastikan(v.dunia.keadaan == GameWorld.SIAP, "awal harus layar pembuka");
        v.onTouchEvent(new MotionEvent(MotionEvent.ACTION_UP));
        pastikan(v.dunia.keadaan == GameWorld.SIAP, "angkat jari tidak boleh memulai");
        v.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN));
        pastikan(v.dunia.keadaan == GameWorld.MAIN, "sentuhan harus memulai permainan");

        v.saatLanjut();
        v.onDraw(new Canvas());
        Thread.sleep(30);
        v.onDraw(new Canvas());
        float waktu = v.dunia.waktu;
        pastikan(waktu > 0.01f, "permainan harus berjalan saat digambar");

        v.saatDijeda();
        v.saatLanjut();
        v.onDraw(new Canvas());
        Thread.sleep(30);
        v.onDraw(new Canvas());
        pastikan(v.dunia.waktu == waktu, "saat dijeda permainan harus berhenti");
        v.onTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN));
        v.onDraw(new Canvas());
        Thread.sleep(30);
        v.onDraw(new Canvas());
        pastikan(v.dunia.waktu > waktu, "ketukan harus melanjutkan permainan");
        pastikan(v.dunia.lompatanTerpakai == 0, "ketukan pelanjut tidak boleh sekaligus melompat");

        // skor terbaik tersimpan dan terbaca oleh tampilan baru
        while (v.dunia.keadaan == GameWorld.MAIN) {
            v.langkah(DT);
        }
        int terbaik = v.dunia.terbaik;
        pastikan(terbaik > 0, "skor terbaik harus terisi");
        pastikan(new GameView(konteks).dunia.terbaik == terbaik, "skor terbaik harus tersimpan");
        System.out.println("sentuhan, jeda, dan simpan skor: beres");
    }

    /** Berapa longgar waktu ketuk untuk tiap rintangan dengan satu lompatan biasa. */
    private static void cetakKelonggaran() {
        String[] nama = {"pot", "peti", "pot kembar", "tinggi"};
        float[] mulaiMuncul = {0f, 0f, 8f, 22f};
        for (int j = 0; j < 4; j++) {
            float h = GameWorld.TINGGI_RINTANGAN[j] - 5f;
            float w = GameWorld.LEBAR_RINTANGAN[j] - 10f + GameWorld.LEBAR_PEMAIN;
            double akar = Math.sqrt(GameWorld.LOMPAT * GameWorld.LOMPAT - 2.0 * GameWorld.GRAVITASI * h);
            double diAtas = 2.0 * akar / GameWorld.GRAVITASI;
            float lajuAwal = Math.min(GameWorld.LAJU_MAKS, GameWorld.LAJU_AWAL + GameWorld.PERCEPATAN * mulaiMuncul[j]);
            double longgarAwal = diAtas - w / lajuAwal;
            double longgarAkhir = diAtas - w / GameWorld.LAJU_MAKS;
            System.out.printf(Locale.ROOT, "kelonggaran %-10s: %.0f ms saat pertama muncul, %.0f ms di laju tertinggi%n",
                    nama[j], longgarAwal * 1000, longgarAkhir * 1000);
            pastikan(longgarAwal > 0.18, "rintangan " + nama[j] + " terlalu sulit untuk satu lompatan");
        }
    }

    /** Pemain otomatis. galatMaks = ketidaktepatan waktu ketuk dalam detik. */
    private static final class Bot {
        private final float galatMaks;
        private final Random acak;
        private final IdentityHashMap<GameWorld.Rintangan, Float> galat = new IdentityHashMap<>();

        Bot(float galatMaks, long benih) {
            this.galatMaks = galatMaks;
            this.acak = new Random(benih * 31 + 5);
        }

        void main(GameWorld d) {
            if (d.keadaan != GameWorld.MAIN || d.lompatanTerpakai != 0) {
                return;
            }
            GameWorld.Rintangan dekat = null;
            for (GameWorld.Rintangan r : d.rintangan) {
                boolean belumLewat = r.x + GameWorld.LEBAR_RINTANGAN[r.jenis] - 5f > d.xPemain;
                if (belumLewat && (dekat == null || r.x < dekat.x)) {
                    dekat = r;
                }
            }
            if (dekat == null) {
                return;
            }
            Float g = galat.get(dekat);
            if (g == null) {
                g = (acak.nextFloat() * 2f - 1f) * galatMaks;
                if (galat.size() > 50) {
                    galat.clear();
                }
                galat.put(dekat, g);
            }
            // melompat supaya puncak lompatan jatuh tepat di tengah rintangan
            float w = GameWorld.LEBAR_RINTANGAN[dekat.jenis];
            float masuk = (dekat.x + 5f - (d.xPemain + GameWorld.LEBAR_PEMAIN)) / d.laju;
            float keluar = (dekat.x + w - 5f - d.xPemain) / d.laju;
            float puncak = GameWorld.LOMPAT / GameWorld.GRAVITASI;
            if ((masuk + keluar) / 2f <= puncak + g) {
                d.ketuk();
            }
        }
    }

    private static GameWorld duniaBaru(long benih) {
        GameWorld d = new GameWorld(benih);
        d.lebarLayar = benih % 3 == 0 ? 560f : benih % 3 == 1 ? 640f : 800f;
        d.xPemain = Math.max(64f, d.lebarLayar * 0.14f);
        return d;
    }

    private static void ujiBotSempurna() {
        for (float dt : new float[] {1f / 30f, 1f / 60f, 1f / 120f}) {
            int mati = 0;
            long skor = 0;
            long bunga = 0;
            int[] jenis = new int[4];
            float lajuTertinggi = 0f;
            for (long benih = 1; benih <= 300; benih++) {
                GameWorld d = duniaBaru(benih);
                Bot bot = new Bot(0f, benih);
                d.ketuk();
                Object terakhir = null;
                for (float t = 0f; t < 120f && d.keadaan == GameWorld.MAIN; t += dt) {
                    bot.main(d);
                    d.maju(dt);
                    pastikan(d.skor == (int) (d.jarak / 12f) + d.bungaTerkumpul * GameWorld.NILAI_BUNGA,
                            "skor tidak konsisten");
                    pastikan(d.rintangan.size() < 12 && d.bunga.size() < 12, "benda menumpuk di layar");
                    if (!d.rintangan.isEmpty()) {
                        GameWorld.Rintangan baru = d.rintangan.get(d.rintangan.size() - 1);
                        if (baru != terakhir) {
                            terakhir = baru;
                            jenis[baru.jenis]++;
                        }
                    }
                    lajuTertinggi = Math.max(lajuTertinggi, d.laju);
                }
                if (d.keadaan != GameWorld.MAIN) {
                    mati++;
                }
                skor += d.skor;
                bunga += d.bungaTerkumpul;
            }
            System.out.printf(Locale.ROOT,
                    "bot sempurna @%3.0f fps: %d/300 ronde gagal dalam 2 menit; rata-rata skor %d, bunga %d; "
                            + "rintangan pot/peti/kembar/tinggi %d/%d/%d/%d; laju tertinggi %.0f%n",
                    1f / dt, mati, skor / 300, bunga / 300, jenis[0], jenis[1], jenis[2], jenis[3], lajuTertinggi);
            pastikan(mati == 0, "ada susunan rintangan yang mustahil dilewati");
        }
    }

    private static void ujiBotCeroboh() {
        for (float galat : new float[] {0.06f, 0.10f, 0.15f, 0.20f}) {
            int selamat = 0;
            double lamaTotal = 0;
            for (long benih = 1; benih <= 200; benih++) {
                GameWorld d = duniaBaru(benih);
                Bot bot = new Bot(galat, benih);
                d.ketuk();
                float t = 0f;
                while (t < 120f && d.keadaan == GameWorld.MAIN) {
                    bot.main(d);
                    d.maju(DT);
                    t += DT;
                }
                if (d.keadaan == GameWorld.MAIN) {
                    selamat++;
                }
                lamaTotal += t;
            }
            System.out.printf(Locale.ROOT, "bot meleset sampai %3.0f ms: %3d%% bertahan 2 menit, rata-rata %.0f detik%n",
                    galat * 1000, selamat / 2, lamaTotal / 200);
            if (galat <= 0.061f) {
                pastikan(selamat >= 190, "terlalu sulit untuk ketukan yang hampir tepat");
            }
        }
    }

    // ------------------------------------------------------------ gambar

    private static void renderContoh(String keluaran) throws IOException {
        simpan(keluaran, "layar_siap", layar(2400, 1080, 0, LAYAR_SIAP));
        simpan(keluaran, "layar_main", layar(2400, 1080, 0, LAYAR_MAIN));
        simpan(keluaran, "layar_kalah", layar(2400, 1080, 0, LAYAR_KALAH));
        simpan(keluaran, "layar_jeda", layar(2400, 1080, 0, LAYAR_JEDA));
        simpan(keluaran, "layar_169", layar(1920, 1080, 0, LAYAR_MAIN));
        simpan(keluaran, "layar_poni", layar(2400, 1080, 120, LAYAR_SIAP));
        simpan(keluaran, "layar_tegak", layar(1080, 2400, 0, LAYAR_MAIN));
    }

    private static String layar(int w, int h, int amanKiri, int mode) {
        GameView v = new GameView(new Context());
        v.onSizeChanged(w, h, 0, 0);
        if (amanKiri > 0) {
            v.aturAreaAman(amanKiri, 0, 0);
        }
        for (int i = 0; i < 40; i++) {
            v.langkah(DT);
        }
        if (mode != LAYAR_SIAP) {
            GameWorld d = v.dunia;
            Bot bot = new Bot(0f, 11);
            v.ketuk();
            for (float t = 0f; t < 60f; t += DT) {
                bot.main(d);
                v.langkah(DT);
                boolean momenBagus = d.tinggiPemain > 70f && d.rintangan.size() >= 2 && !d.bunga.isEmpty();
                if (mode != LAYAR_KALAH && t > 9f && momenBagus) {
                    break;
                }
                if (mode == LAYAR_KALAH && t > 14f) {
                    break;
                }
            }
            if (mode == LAYAR_KALAH) {
                while (d.keadaan == GameWorld.MAIN) {
                    v.langkah(DT);
                }
                for (int i = 0; i < 60; i++) {
                    v.langkah(DT);
                }
            }
            if (mode == LAYAR_JEDA) {
                v.saatDijeda();
            }
        }
        Canvas kanvas = new Canvas();
        v.gambar(kanvas);
        return kanvas.keSvg(w, h);
    }

    private static void simpan(String folder, String nama, String svg) throws IOException {
        Files.write(Paths.get(folder, nama + ".svg"), svg.getBytes(StandardCharsets.UTF_8));
    }
}
