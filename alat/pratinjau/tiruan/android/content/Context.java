package android.content;

import android.content.res.Resources;

import java.util.HashMap;
import java.util.Map;

/** Tiruan Context: simpanan angka hanya hidup di memori. */
public class Context {

    public static final int MODE_PRIVATE = 0;

    private final Resources sumber = new Resources();
    private final Map<String, Integer> angka = new HashMap<>();

    public Resources getResources() {
        return sumber;
    }

    public SharedPreferences getSharedPreferences(String nama, int mode) {
        return new SharedPreferences() {
            @Override
            public int getInt(String kunci, int bawaan) {
                Integer nilai = angka.get(nama + "/" + kunci);
                return nilai == null ? bawaan : nilai;
            }

            @Override
            public Editor edit() {
                return new Editor() {
                    @Override
                    public Editor putInt(String kunci, int nilai) {
                        angka.put(nama + "/" + kunci, nilai);
                        return this;
                    }

                    @Override
                    public void apply() {
                    }
                };
            }
        };
    }
}
