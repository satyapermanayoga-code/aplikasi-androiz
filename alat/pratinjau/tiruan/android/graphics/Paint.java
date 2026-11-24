package android.graphics;

/** Tiruan Paint: hanya menyimpan pengaturan yang dipakai game. */
public class Paint {

    public static final int ANTI_ALIAS_FLAG = 1;
    public static final int FILTER_BITMAP_FLAG = 2;

    public enum Align { LEFT, CENTER, RIGHT }

    int color = 0xFF000000;
    Shader shader;
    float ukuranTeks = 12f;
    Align rata = Align.LEFT;

    public Paint(int bendera) {
    }

    public void setColor(int warna) {
        color = warna;
    }

    public Shader setShader(Shader s) {
        shader = s;
        return s;
    }

    public void setTextSize(float ukuran) {
        ukuranTeks = ukuran;
    }

    public void setTextAlign(Align a) {
        rata = a;
    }

    public Typeface setTypeface(Typeface t) {
        return t;
    }
}
