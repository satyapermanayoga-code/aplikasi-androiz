package android.graphics;

import android.content.res.Resources;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;

/** Tiruan BitmapFactory: mencari file PNG dari nama di kelas R tiruan. */
public class BitmapFactory {

    public static Bitmap decodeResource(Resources res, int id) {
        try {
            for (Field f : Class.forName("com.proyekapk.lompat.R$drawable").getFields()) {
                if (f.getInt(null) == id) {
                    String berkas = System.getProperty("pratinjau.res") + "/drawable-nodpi/" + f.getName() + ".png";
                    try (DataInputStream masuk = new DataInputStream(new FileInputStream(berkas))) {
                        masuk.skipBytes(16); // tanda PNG + kepala IHDR
                        return new Bitmap(f.getName(), masuk.readInt(), masuk.readInt());
                    }
                }
            }
        } catch (ReflectiveOperationException | IOException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalArgumentException("Gambar tidak ditemukan: " + id);
    }
}
