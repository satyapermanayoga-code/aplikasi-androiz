package android.graphics;

/** Tiruan Bitmap: cukup nama file dan ukurannya. */
public class Bitmap {

    final String nama;
    private final int lebar;
    private final int tinggi;

    Bitmap(String nama, int lebar, int tinggi) {
        this.nama = nama;
        this.lebar = lebar;
        this.tinggi = tinggi;
    }

    public int getWidth() {
        return lebar;
    }

    public int getHeight() {
        return tinggi;
    }
}
