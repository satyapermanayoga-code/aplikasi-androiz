package android.graphics;

public class LinearGradient extends Shader {

    final float x0;
    final float y0;
    final float x1;
    final float y1;
    final int warna0;
    final int warna1;

    public LinearGradient(float x0, float y0, float x1, float y1, int warna0, int warna1, TileMode mode) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.warna0 = warna0;
        this.warna1 = warna1;
    }
}
