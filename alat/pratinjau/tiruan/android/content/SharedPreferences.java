package android.content;

public interface SharedPreferences {

    int getInt(String kunci, int bawaan);

    Editor edit();

    interface Editor {

        Editor putInt(String kunci, int nilai);

        void apply();
    }
}
