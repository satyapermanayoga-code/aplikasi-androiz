package android.view;

public class MotionEvent {

    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;

    private final int aksi;

    public MotionEvent(int aksi) {
        this.aksi = aksi;
    }

    public int getActionMasked() {
        return aksi;
    }
}
