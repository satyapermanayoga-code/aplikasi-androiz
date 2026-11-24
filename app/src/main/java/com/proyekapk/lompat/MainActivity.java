package com.proyekapk.lompat;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.DisplayCutout;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

/** Satu-satunya layar: permainan layar penuh, posisi mendatar. */
public class MainActivity extends Activity {

    private GameView game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window jendela = getWindow();
        jendela.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28) {
            // gambar sampai ke bawah poni kamera; isi penting digeser lewat aturAreaAman
            WindowManager.LayoutParams atribut = jendela.getAttributes();
            atribut.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            jendela.setAttributes(atribut);
        }

        game = new GameView(this);
        setContentView(game);

        game.setOnApplyWindowInsetsListener((tampilan, sisipan) -> {
            int kiri;
            int atas;
            int kanan;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets i = sisipan.getInsets(WindowInsets.Type.displayCutout() | WindowInsets.Type.systemBars());
                kiri = i.left;
                atas = i.top;
                kanan = i.right;
            } else {
                kiri = sisipan.getSystemWindowInsetLeft();
                atas = sisipan.getSystemWindowInsetTop();
                kanan = sisipan.getSystemWindowInsetRight();
                if (Build.VERSION.SDK_INT >= 28) {
                    DisplayCutout poni = sisipan.getDisplayCutout();
                    if (poni != null) {
                        kiri = Math.max(kiri, poni.getSafeInsetLeft());
                        atas = Math.max(atas, poni.getSafeInsetTop());
                        kanan = Math.max(kanan, poni.getSafeInsetRight());
                    }
                }
            }
            game.aturAreaAman(kiri, atas, kanan);
            return sisipan;
        });

        sembunyikanBilahSistem();
    }

    @Override
    protected void onResume() {
        super.onResume();
        game.saatLanjut();
    }

    @Override
    protected void onPause() {
        game.saatDijeda();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean adaFokus) {
        super.onWindowFocusChanged(adaFokus);
        if (adaFokus) {
            sembunyikanBilahSistem();
        }
    }

    @SuppressWarnings("deprecation")
    private void sembunyikanBilahSistem() {
        Window jendela = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            jendela.setDecorFitsSystemWindows(false);
            WindowInsetsController pengendali = jendela.getInsetsController();
            if (pengendali != null) {
                pengendali.hide(WindowInsets.Type.systemBars());
                pengendali.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            jendela.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }
}
