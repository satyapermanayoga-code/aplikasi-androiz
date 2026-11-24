package android.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;

/** Tiruan View: tanpa layar sungguhan, hanya cukup untuk menjalankan GameView. */
public class View {

    private final Context konteks;

    public View(Context konteks) {
        this.konteks = konteks;
    }

    public Context getContext() {
        return konteks;
    }

    public Resources getResources() {
        return konteks.getResources();
    }

    public void invalidate() {
    }

    public void postInvalidateOnAnimation() {
    }

    public boolean performHapticFeedback(int jenis) {
        return true;
    }

    protected void onDraw(Canvas kanvas) {
    }

    protected void onSizeChanged(int w, int h, int lamaW, int lamaH) {
    }

    public boolean onTouchEvent(MotionEvent kejadian) {
        return false;
    }
}
