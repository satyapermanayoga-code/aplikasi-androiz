package com.proyekapk.halodunia;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

/** Layar tunggal: sapaan, tombol penghitung ketukan, dan nomor versi. */
public class MainActivity extends Activity {

    private static final String KEY_COUNT = "count";

    private int count = 0;
    private TextView counterText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        counterText = findViewById(R.id.counter_text);
        Button tapButton = findViewById(R.id.tap_button);
        TextView versionText = findViewById(R.id.version_text);

        // Pulihkan hitungan setelah layar diputar.
        if (savedInstanceState != null) {
            count = savedInstanceState.getInt(KEY_COUNT, 0);
        }
        showCount();

        tapButton.setOnClickListener(view -> {
            count++;
            showCount();
        });

        versionText.setText(getString(R.string.version_label, readVersionName()));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_COUNT, count);
    }

    private void showCount() {
        counterText.setText(getString(R.string.counter_label, count));
    }

    private String readVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }
}
