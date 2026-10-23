package com.example.dinodate;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private TextView status;
    private final Shizuku.OnRequestPermissionResultListener listener = (requestCode, grantResult) -> updateStatus();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Shizuku.addRequestPermissionResultListener(listener);
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(36, 50, 36, 32);
        scroll.addView(panel);
        TextView intro = new TextView(this);
        intro.setText("DINO DATE AUTO · Android 15\n\nDetecta la pantalla DINO HUB / Bitcoin Valley usando Accesibilidad. Intenta adelantar 1 día mediante Shizuku. Si Android no lo permite, te avisa para cambiar la fecha manualmente.\n\nAVISO: cambiar la fecha afecta alarmas, certificados y otras aplicaciones. Este programa no evita restricciones de servidores. Desactiva Fecha y hora automáticas primero.");
        intro.setTextSize(17); panel.addView(intro);
        status = new TextView(this); status.setTextSize(16); status.setPadding(0, 24, 0, 24); panel.addView(status);
        addButton(panel, "1 · Abrir Accesibilidad", () -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        addButton(panel, "2 · Abrir Fecha y hora", () -> startActivity(new Intent(Settings.ACTION_DATE_SETTINGS)));
        addButton(panel, "3 · Autorizar Shizuku", () -> {
            try {
                if (!Shizuku.pingBinder()) { status.setText("Iniciá Shizuku primero (depuración inalámbrica)."); return; }
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) Shizuku.requestPermission(101);
                else updateStatus();
            } catch (Exception e) { status.setText("Shizuku no está disponible: " + e.getMessage()); }
        });
        Button toggle = new Button(this);
        panel.addView(toggle);
        Runnable redraw = () -> toggle.setText(enabled() ? "4 · DESACTIVAR detector" : "4 · ACTIVAR detector");
        toggle.setOnClickListener(v -> {
            getSharedPreferences("prefs", MODE_PRIVATE).edit().putBoolean("enabled", !enabled()).apply();
            redraw.run(); updateStatus();
        });
        redraw.run();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 77);
        setContentView(scroll); updateStatus();
    }
    private void addButton(LinearLayout panel, String title, Runnable action) {
        Button b = new Button(this); b.setText(title); b.setOnClickListener(v -> action.run()); panel.addView(b);
    }
    private boolean enabled() { return getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("enabled", false); }
    private void updateStatus() {
        String shizuku;
        try { shizuku = !Shizuku.pingBinder() ? "No iniciado" : Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED ? "Autorizado" : "Sin autorización"; }
        catch (Exception e) { shizuku = "No disponible"; }
        status.setText("Detector: " + (enabled() ? "activado" : "desactivado") + "\nShizuku: " + shizuku + "\nÚltimo resultado: " + getSharedPreferences("prefs", MODE_PRIVATE).getString("result", "Sin intentos"));
    }
    @Override protected void onResume() { super.onResume(); if (status != null) updateStatus(); }
    @Override protected void onDestroy() { Shizuku.removeRequestPermissionResultListener(listener); super.onDestroy(); }
}
