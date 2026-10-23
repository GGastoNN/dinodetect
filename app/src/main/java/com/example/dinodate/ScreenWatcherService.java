package com.example.dinodate;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.time.ZonedDateTime;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

public class ScreenWatcherService extends AccessibilityService {
    private static final String TARGET = "com.emgruppen.bitcoindino";
    private static final String CHANNEL = "date_manual";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile boolean triggered = false;
    private volatile boolean busy = false;
    private long lastAttemptElapsed = 0;

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("enabled", false)) return;
        CharSequence pkg = event.getPackageName();
        if (pkg == null || !TARGET.contentEquals(pkg)) { triggered = false; return; }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        StringBuilder text = new StringBuilder();
        extract(root, text, 0);
        String page = text.toString().toLowerCase(Locale.ROOT);
        // Rendered games often don't expose text to Accessibility; this is text detection, not image matching.
        boolean match = page.contains("dino hub") && page.contains("bitcoin valley") && page.contains("power-ups");
        if (!match) { triggered = false; return; }
        long now = SystemClock.elapsedRealtime();
        if (triggered || busy || now - lastAttemptElapsed < 60000) return;
        triggered = true;
        busy = true;
        lastAttemptElapsed = now;
        worker.execute(() -> {
            try { attemptDateChange(); }
            catch (Exception e) { fallback("Error al intentar el cambio: " + e.getClass().getSimpleName()); }
            finally { busy = false; }
        });
    }
    private void attemptDateChange() {
        if (Settings.Global.getInt(getContentResolver(), Settings.Global.AUTO_TIME, 1) != 0) {
            fallback("Desactivá Fecha y hora automáticas antes de continuar"); return;
        }
        try {
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                fallback("Shizuku no está iniciado o autorizado"); return;
            }
            long before = System.currentTimeMillis();
            long target = ZonedDateTime.now().plusDays(1).toInstant().toEpochMilli();
            // Shizuku on unrooted phones runs as shell (UID 2000), NOT as root.
            // This command may be unsupported or denied by Android/HyperOS.
            // Deprecated Shizuku process API: accessed reflectively for compatibility with 13.1.5.
            Method method = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            method.setAccessible(true);
            ShizukuRemoteProcess proc = (ShizukuRemoteProcess) method.invoke(null, new String[]{"cmd", "alarm", "set-time", Long.toString(target)}, null, null);
            int code = proc.waitFor();
            // Verify wall-clock advance independently. Exit code alone is not proof.
            long after = System.currentTimeMillis();
            boolean changed = code == 0 && after - before > 20L * 60 * 60 * 1000L;
            if (changed) {
                save("Fecha adelantada; comprobada en el reloj del sistema");
                showNotification("Fecha cambiada", "Se adelantó un día en el sistema", false);
            } else fallback("Android/HyperOS no permitió adelantar la fecha (código " + code + ")");
        } catch (Exception | LinkageError e) { fallback("Shizuku no pudo ejecutar el cambio: " + e.getClass().getSimpleName()); }
    }
    private void fallback(String why) {
        save(why);
        showNotification("Cambio manual necesario", why + ". Tocá para abrir Fecha y hora.", true);
    }
    private void save(String result) { getSharedPreferences("prefs", MODE_PRIVATE).edit().putString("result", result).apply(); }
    private void showNotification(String title, String message, boolean openSettings) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Avisos de fecha", NotificationManager.IMPORTANCE_HIGH));
        Intent intent = openSettings ? new Intent(Settings.ACTION_DATE_SETTINGS) : new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification note = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(pi).setAutoCancel(true).build();
        manager.notify(200, note);
    }
    private void extract(AccessibilityNodeInfo n, StringBuilder out, int depth) {
        if (n == null || depth > 30 || out.length() > 12000) return;
        if (n.getText() != null) out.append(' ').append(n.getText());
        if (n.getContentDescription() != null) out.append(' ').append(n.getContentDescription());
        for (int i = 0; i < n.getChildCount(); ++i) extract(n.getChild(i), out, depth + 1);
    }
    @Override public void onInterrupt() {}
    @Override public void onDestroy() { worker.shutdownNow(); super.onDestroy(); }
}
