package com.proyekapk.lompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * Menggambar permainan dan menerima ketukan. Semua ukuran memakai satuan
 * GameWorld (360 satuan = tinggi area main); kanvas diskalakan sekali di awal.
 */
final class GameView extends View {

    private static final String NAMA_SIMPANAN = "lompat";
    private static final String KUNCI_TERBAIK = "terbaik";

    /** Lebar area main paling sempit, supaya tetap bisa dimainkan di layar tegak. */
    private static final float LEBAR_MIN = 560f;
    private static final float UMUR_EFEK = 0.7f;

    private static final int LANGIT_ATAS = 0xFF9FD8FF;
    private static final int LANGIT_BAWAH = 0xFFFFF4E3;
    private static final int TINTA = 0xFF3A2A34;
    private static final int TINTA_PUDAR = 0xFF7A6B74;
    private static final int MERAH_MUDA = 0xFFD6336C;

    final GameWorld dunia = new GameWorld(System.nanoTime());

    private final Paint cat = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint catGambar = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint catTeks = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF kotak = new RectF();

    private final Bitmap lari1;
    private final Bitmap lari2;
    private final Bitmap lompat;
    private final Bitmap jatuh;
    private final Bitmap pot;
    private final Bitmap peti;
    private final Bitmap tinggi;
    private final Bitmap bunga;
    private final SharedPreferences simpanan;

    private int lebarPx;
    private int tinggiPx;
    private int amanKiriPx;
    private int amanAtasPx;
    private int amanKananPx;

    /** Piksel per satuan. */
    private float skala = 1f;
    /** Lebar layar dalam satuan. */
    private float lebar = 640f;
    /** Ruang langit tambahan di atas area main (hanya di layar tegak). */
    private float ekstraAtas;
    private float amanKiri;
    private float amanAtas;
    private float amanKanan;
    private Shader langit;

    private long waktuTerakhir;
    private boolean jeda;
    private boolean berjalan;

    private float efekX;
    private float efekTinggi;
    private float efekUmur = -1f;

    GameView(Context konteks) {
        super(konteks);
        lari1 = BitmapFactory.decodeResource(getResources(), R.drawable.avatar_lari1);
        lari2 = BitmapFactory.decodeResource(getResources(), R.drawable.avatar_lari2);
        lompat = BitmapFactory.decodeResource(getResources(), R.drawable.avatar_lompat);
        jatuh = BitmapFactory.decodeResource(getResources(), R.drawable.avatar_jatuh);
        pot = BitmapFactory.decodeResource(getResources(), R.drawable.rintangan_pot);
        peti = BitmapFactory.decodeResource(getResources(), R.drawable.rintangan_peti);
        tinggi = BitmapFactory.decodeResource(getResources(), R.drawable.rintangan_tinggi);
        bunga = BitmapFactory.decodeResource(getResources(), R.drawable.bunga);
        catTeks.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        simpanan = konteks.getSharedPreferences(NAMA_SIMPANAN, Context.MODE_PRIVATE);
        dunia.terbaik = simpanan.getInt(KUNCI_TERBAIK, 0);
    }

    // ------------------------------------------------------------ ukuran

    @Override
    protected void onSizeChanged(int w, int h, int lamaW, int lamaH) {
        super.onSizeChanged(w, h, lamaW, lamaH);
        lebarPx = w;
        tinggiPx = h;
        hitungUkuran();
    }

    /** Area yang tertutup poni kamera atau bilah sistem, dalam piksel. */
    void aturAreaAman(int kiriPx, int atasPx, int kananPx) {
        amanKiriPx = kiriPx;
        amanAtasPx = atasPx;
        amanKananPx = kananPx;
        hitungUkuran();
    }

    private void hitungUkuran() {
        if (lebarPx <= 0 || tinggiPx <= 0) {
            return;
        }
        skala = Math.min(tinggiPx / GameWorld.TINGGI_LAYAR, lebarPx / LEBAR_MIN);
        lebar = lebarPx / skala;
        ekstraAtas = tinggiPx / skala - GameWorld.TINGGI_LAYAR;
        amanKiri = amanKiriPx / skala;
        amanAtas = amanAtasPx / skala;
        amanKanan = amanKananPx / skala;
        dunia.lebarLayar = lebar;
        dunia.xPemain = Math.max(64f, lebar * 0.14f) + amanKiri;
        langit = new LinearGradient(0f, 0f, 0f, GameWorld.Y_TANAH, LANGIT_ATAS, LANGIT_BAWAH,
                Shader.TileMode.CLAMP);
    }

    // ---------------------------------------------------- putaran gambar

    void saatLanjut() {
        berjalan = true;
        waktuTerakhir = 0L;
        invalidate();
    }

    void saatDijeda() {
        berjalan = false;
        if (dunia.keadaan == GameWorld.MAIN) {
            jeda = true;
        }
    }

    @Override
    protected void onDraw(Canvas kanvas) {
        long kini = System.nanoTime();
        float dt = waktuTerakhir == 0L ? 0f : (kini - waktuTerakhir) / 1_000_000_000f;
        waktuTerakhir = kini;
        if (!jeda) {
            langkah(dt);
        }
        gambar(kanvas);
        if (berjalan) {
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent kejadian) {
        if (kejadian.getActionMasked() == MotionEvent.ACTION_DOWN) {
            ketuk();
        }
        return true;
    }

    void ketuk() {
        if (jeda) {
            jeda = false;
            waktuTerakhir = 0L;
        } else {
            dunia.ketuk();
        }
    }

    /** Memajukan permainan dan menanggapi kejadian (bunga terambil, kalah). */
    void langkah(float dt) {
        dunia.maju(dt);
        if (dunia.baruAmbilBunga) {
            dunia.baruAmbilBunga = false;
            efekX = dunia.xBungaTerakhir;
            efekTinggi = dunia.tinggiBungaTerakhir;
            efekUmur = 0f;
        }
        if (efekUmur >= 0f) {
            efekUmur += dt;
            if (efekUmur > UMUR_EFEK) {
                efekUmur = -1f;
            }
        }
        if (dunia.baruKalah) {
            dunia.baruKalah = false;
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            if (dunia.rekorBaru) {
                simpanan.edit().putInt(KUNCI_TERBAIK, dunia.terbaik).apply();
            }
        }
    }

    void gambar(Canvas c) {
        if (langit == null) {
            return;
        }
        c.save();
        c.scale(skala, skala);
        if (ekstraAtas > 0f) {
            cat.setColor(LANGIT_ATAS);
            c.drawRect(0f, 0f, lebar, ekstraAtas + 1f, cat);
            c.translate(0f, ekstraAtas);
        }
        gambarLatar(c);
        gambarBunga(c);
        gambarRintangan(c);
        gambarPemain(c);
        gambarEfek(c);
        gambarAngka(c);
        gambarPanel(c);
        c.restore();
    }

    // --------------------------------------------------------------- latar

    private void gambarLatar(Canvas c) {
        float yT = GameWorld.Y_TANAH;
        float g = dunia.gulir;

        cat.setShader(langit);
        c.drawRect(0f, 0f, lebar, yT, cat);
        cat.setShader(null);

        cat.setColor(0x33FFFFFF);
        c.drawCircle(lebar - 96f, 78f, 44f, cat);
        cat.setColor(0x4DFFFFFF);
        c.drawCircle(lebar - 96f, 78f, 34f, cat);
        cat.setColor(0xFFFFE680);
        c.drawCircle(lebar - 96f, 78f, 26f, cat);

        gambarAwan(c, g * 0.05f);

        cat.setColor(0xFFC4E6BE);
        gambarBukit(c, g * 0.12f, 300f, 170f, 78f);
        cat.setColor(0xFF9FD598);
        gambarBukit(c, g * 0.28f, 210f, 120f, 46f);

        cat.setColor(0xFF7CC06A);
        c.drawRect(0f, yT, lebar, yT + 10f, cat);
        cat.setColor(0xFF69AF58);
        c.drawRect(0f, yT + 10f, lebar, yT + 13f, cat);
        cat.setColor(0xFFEBCFA3);
        c.drawRect(0f, yT + 13f, lebar, GameWorld.TINGGI_LAYAR, cat);

        // garis jalan dan rumput ikut bergeser, memberi kesan bergerak
        cat.setColor(0xFFDDBB88);
        for (float x = -(g % 96f); x < lebar; x += 96f) {
            kotak.set(x, yT + 30f, x + 40f, yT + 35f);
            c.drawRoundRect(kotak, 2.5f, 2.5f, cat);
            kotak.set(x + 52f, yT + 48f, x + 76f, yT + 52f);
            c.drawRoundRect(kotak, 2f, 2f, cat);
        }
        cat.setColor(0xFF5FA650);
        for (float x = -(g % 58f); x < lebar; x += 58f) {
            c.drawRect(x, yT - 3f, x + 2f, yT, cat);
            c.drawRect(x + 4f, yT - 5f, x + 6f, yT, cat);
            c.drawRect(x + 8f, yT - 2.5f, x + 10f, yT, cat);
        }
    }

    private void gambarAwan(Canvas c, float geser) {
        final float jarak = 260f;
        int awal = (int) Math.floor(geser / jarak);
        float x0 = -(geser - awal * jarak);
        cat.setColor(0xF2FFFFFF);
        for (int k = -1; x0 + k * jarak < lebar + 80f; k++) {
            int pola = (((awal + k) % 3) + 3) % 3;
            float cx = x0 + k * jarak + 120f;
            float cy = pola == 0 ? 52f : pola == 1 ? 96f : 34f;
            float u = pola == 1 ? 0.75f : 1f;
            kotak.set(cx - 34f * u, cy - 2f * u, cx + 34f * u, cy + 14f * u);
            c.drawRoundRect(kotak, 8f * u, 8f * u, cat);
            c.drawCircle(cx - 14f * u, cy - 2f * u, 13f * u, cat);
            c.drawCircle(cx + 6f * u, cy - 8f * u, 17f * u, cat);
            c.drawCircle(cx + 22f * u, cy + 1f * u, 10f * u, cat);
        }
    }

    private void gambarBukit(Canvas c, float geser, float jarak, float rx, float ry) {
        int awal = (int) Math.floor(geser / jarak);
        float x0 = -(geser - awal * jarak);
        float dasar = GameWorld.Y_TANAH + 10f;
        for (int k = -1; x0 + k * jarak - rx * 1.2f < lebar; k++) {
            float f = 0.8f + 0.2f * ((((awal + k) % 3) + 3) % 3);
            float cx = x0 + k * jarak + jarak * 0.5f;
            kotak.set(cx - rx * f, dasar - ry * f, cx + rx * f, dasar + ry * f);
            c.drawOval(kotak, cat);
        }
    }

    // ------------------------------------------------------- benda dan pemain

    private void gambarBunga(Canvas c) {
        float jari = GameWorld.UKURAN_BUNGA / 2f;
        for (int i = 0; i < dunia.bunga.size(); i++) {
            GameWorld.Bunga b = dunia.bunga.get(i);
            float naik = (float) Math.sin(dunia.waktu * 4f + i) * 2.5f;
            float cy = GameWorld.Y_TANAH - b.tinggi + naik;
            kotak.set(b.x - jari, cy - jari, b.x + jari, cy + jari);
            c.drawBitmap(bunga, null, kotak, catGambar);
        }
    }

    private void gambarRintangan(Canvas c) {
        float yT = GameWorld.Y_TANAH;
        for (int i = 0; i < dunia.rintangan.size(); i++) {
            GameWorld.Rintangan r = dunia.rintangan.get(i);
            float w = GameWorld.LEBAR_RINTANGAN[r.jenis];
            float h = GameWorld.TINGGI_RINTANGAN[r.jenis];
            cat.setColor(0x26000000);
            kotak.set(r.x - 3f, yT - 2f, r.x + w + 3f, yT + 6f);
            c.drawOval(kotak, cat);
            if (r.jenis == GameWorld.POT_KEMBAR) {
                kotak.set(r.x, yT - h, r.x + 34f, yT);
                c.drawBitmap(pot, null, kotak, catGambar);
                kotak.set(r.x + 36f, yT - h, r.x + 70f, yT);
                c.drawBitmap(pot, null, kotak, catGambar);
            } else {
                Bitmap gambar = r.jenis == GameWorld.POT ? pot : r.jenis == GameWorld.PETI ? peti : tinggi;
                kotak.set(r.x, yT - h, r.x + w, yT);
                c.drawBitmap(gambar, null, kotak, catGambar);
            }
        }
    }

    private void gambarPemain(Canvas c) {
        float yT = GameWorld.Y_TANAH;
        float tengah = dunia.xPemain + GameWorld.LEBAR_PEMAIN / 2f;
        float kaki = yT - dunia.tinggiPemain;

        // bayangan mengecil saat pemain naik
        float s = 1f - Math.min(1f, dunia.tinggiPemain / 170f) * 0.55f;
        cat.setColor(0x30000000);
        kotak.set(tengah - 24f * s, yT, tengah + 24f * s, yT + 9f * s);
        c.drawOval(kotak, cat);

        Bitmap gambar;
        float ayun = 0f;
        if (dunia.keadaan == GameWorld.KALAH) {
            gambar = jatuh;
        } else if (dunia.lompatanTerpakai > 0) {
            gambar = lompat;
        } else if (dunia.keadaan == GameWorld.SIAP) {
            gambar = lari1;
            ayun = (float) Math.sin(dunia.gulir * 0.12f) * 1.5f;
        } else {
            gambar = ((int) (dunia.jarak / 28f)) % 2 == 0 ? lari1 : lari2;
        }

        // gambar avatar 96 x 120 satuan, telapak kaki di baris ke-118
        kotak.set(tengah - 48f, kaki - 118f + ayun, tengah + 48f, kaki + 2f + ayun);
        if (dunia.keadaan == GameWorld.KALAH) {
            c.save();
            c.rotate(-14f, tengah, kaki);
            c.drawBitmap(gambar, null, kotak, catGambar);
            c.restore();
        } else {
            c.drawBitmap(gambar, null, kotak, catGambar);
        }
    }

    private void gambarEfek(Canvas c) {
        if (efekUmur < 0f) {
            return;
        }
        float t = efekUmur / UMUR_EFEK;
        int alfa = (int) (255f * (1f - t));
        float y = GameWorld.Y_TANAH - efekTinggi - 18f - 30f * t;
        teks(c, "+" + GameWorld.NILAI_BUNGA, efekX, y, 18f, (alfa << 24) | (MERAH_MUDA & 0x00FFFFFF),
                Paint.Align.CENTER);
    }

    // ------------------------------------------------------- angka dan panel

    private void gambarAngka(Canvas c) {
        float atas = -ekstraAtas + amanAtas;
        float kiri = 16f + amanKiri;
        float kanan = lebar - 16f - amanKanan;
        teks(c, "Skor " + dunia.skor, kiri, atas + 32f, 22f, TINTA, Paint.Align.LEFT);
        kotak.set(kiri, atas + 41f, kiri + 18f, atas + 59f);
        c.drawBitmap(bunga, null, kotak, catGambar);
        teks(c, String.valueOf(dunia.bungaTerkumpul), kiri + 24f, atas + 56f, 16f, TINTA, Paint.Align.LEFT);
        teks(c, "Terbaik " + dunia.terbaik, kanan, atas + 32f, 18f, TINTA, Paint.Align.RIGHT);
    }

    private void gambarPanel(Canvas c) {
        float cx = lebar * 0.56f;
        float cy = 140f;
        if (jeda) {
            latarPanel(c, cx, cy, 280f, 104f);
            teks(c, "Jeda", cx, cy - 6f, 28f, TINTA, Paint.Align.CENTER);
            teks(c, "Ketuk untuk lanjut", cx, cy + 26f, 16f, TINTA_PUDAR, Paint.Align.CENTER);
        } else if (dunia.keadaan == GameWorld.SIAP) {
            latarPanel(c, cx, cy, 392f, 156f);
            teks(c, "Lompat-Lompat", cx, cy - 30f, 34f, TINTA, Paint.Align.CENTER);
            teks(c, "Ketuk layar untuk mulai", cx, cy + 4f, 18f, MERAH_MUDA, Paint.Align.CENTER);
            teks(c, "Ketuk untuk melompati rintangan.", cx, cy + 32f, 13f, TINTA_PUDAR, Paint.Align.CENTER);
            teks(c, "Ketuk sekali lagi di udara untuk lompat ganda.", cx, cy + 51f, 13f, TINTA_PUDAR,
                    Paint.Align.CENTER);
        } else if (dunia.keadaan == GameWorld.KALAH) {
            latarPanel(c, cx, cy, 320f, 176f);
            teks(c, "Yah, tersandung!", cx, cy - 46f, 24f, TINTA, Paint.Align.CENTER);
            teks(c, "Skor " + dunia.skor, cx, cy - 6f, 32f, TINTA, Paint.Align.CENTER);
            if (dunia.rekorBaru) {
                teks(c, "Rekor baru!", cx, cy + 24f, 17f, MERAH_MUDA, Paint.Align.CENTER);
            } else {
                teks(c, "Terbaik " + dunia.terbaik, cx, cy + 24f, 17f, TINTA_PUDAR, Paint.Align.CENTER);
            }
            if (dunia.waktuKalah >= GameWorld.JEDA_ULANG) {
                teks(c, "Ketuk untuk main lagi", cx, cy + 58f, 16f, MERAH_MUDA, Paint.Align.CENTER);
            }
        }
    }

    private void latarPanel(Canvas c, float cx, float cy, float w, float h) {
        cat.setColor(0x30000000);
        kotak.set(cx - w / 2f + 2f, cy - h / 2f + 5f, cx + w / 2f + 2f, cy + h / 2f + 5f);
        c.drawRoundRect(kotak, 18f, 18f, cat);
        cat.setColor(0xF2FFFFFF);
        kotak.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
        c.drawRoundRect(kotak, 18f, 18f, cat);
    }

    private void teks(Canvas c, String isi, float x, float y, float ukuran, int warna, Paint.Align rata) {
        catTeks.setTextSize(ukuran);
        catTeks.setTextAlign(rata);
        catTeks.setColor(warna);
        c.drawText(isi, x, y, catTeks);
    }
}
