package tr.hizlitv.home;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/** Foreground-only memory sampling and explicit, audited background cleanup. */
final class MemoryController {
    interface Listener { void update(int usedPercent, long availableMb, String result); }
    private final Context context;
    private final Listener listener;
    private final ActivityManager manager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean active, cleaning, closed;
    private String result = "";
    MemoryController(Context c, Listener listener) {
        context = c.getApplicationContext(); this.listener = listener;
        manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
    }
    private final Runnable tick = new Runnable() {
        public void run() { if (active) { sample(); handler.postDelayed(this, 3000); } }
    };
    void start() { stop(); active = true; tick.run(); }
    void stop() { active = false; handler.removeCallbacks(tick); }
    void close() { stop(); closed = true; handler.removeCallbacksAndMessages(null); }
    private ActivityManager.MemoryInfo info() { ActivityManager.MemoryInfo m = new ActivityManager.MemoryInfo(); manager.getMemoryInfo(m); return m; }
    private void sample() {
        if (closed) return;
        ActivityManager.MemoryInfo m = info();
        int used = m.totalMem <= 0 ? 0 : (int)Math.max(0, Math.min(100, 100 - m.availMem * 100 / m.totalMem));
        listener.update(used, m.availMem / 1048576, result);
    }
    private void log(String event, long before, long after, List<String> targets, int errors) throws Exception {
        JSONObject item = new JSONObject(); item.put("time_ms", System.currentTimeMillis()); item.put("event", event);
        item.put("before_available_bytes", before); item.put("after_available_bytes", after);
        item.put("requested_packages", new JSONArray(targets)); item.put("errors", errors);
        item.put("undo", "Uygulamayı tekrar aç. Veriler silinmedi.");
        File dir = context.getExternalFilesDir(null); if (dir == null) throw new java.io.IOException("No audit directory");
        try (FileWriter writer = new FileWriter(new File(dir, "memory-operations.jsonl"), true)) { writer.write(item + "\n"); }
    }
    void relax(List<AppCatalog.Entry> entries) {
        if (cleaning) return;
        if (Build.VERSION.SDK_INT >= 34) { result = "Bu Android sürümü diğer uygulamaların belleğini boşaltmaya izin vermiyor."; sample(); return; }
        final long before = info().availMem; final ArrayList<String> targets = new ArrayList<>();
        for (AppCatalog.Entry entry : entries) {
            if (AppTargets.protectedFromCleanup(entry.pkg) || entry.pkg.equals(context.getPackageName())) continue;
            try {
                ApplicationInfo app = context.getPackageManager().getApplicationInfo(entry.pkg, 0);
                if ((app.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0) targets.add(entry.pkg);
            } catch (Exception ignored) { }
        }
        cleaning = true; result = "Arka plan belleği kontrol ediliyor…"; sample();
        new Thread(() -> {
            int errors = 0;
            try { log("request", before, -1, targets, 0); }
            catch (Exception failure) {
                handler.post(() -> { cleaning = false; result = "İşlem kaydedilemedi; değişiklik yapılmadı."; sample(); }); return;
            }
            for (String pkg : targets) try { manager.killBackgroundProcesses(pkg); } catch (SecurityException failure) { errors++; }
            final int failures = errors;
            handler.postDelayed(() -> {
                long after = info().availMem;
                result = before / 1048576 + " → " + after / 1048576 + " MB kullanılabilir";
                if (failures > 0) result += " · izin kısıtlı";
                try { log("result", before, after, targets, failures); } catch (Exception failure) { result += " · kayıt hatası"; }
                cleaning = false; sample();
            }, 1500);
        }, "MemoryCleanup").start();
    }
}
