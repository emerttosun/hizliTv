package tr.hizlitv.home;

import android.Manifest;
import tr.hizlitv.ui.TvUi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** User-authorized local ADB diagnostics and selected app closure, without root. */
public final class AppManagerActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ArrayList<Row> rows = new ArrayList<>();
    private ActivityManager am;
    private TextView summary, message;
    private ListView list;
    private BaseAdapter adapter;
    private Button closeSelected, selectCached, clearSelected, refresh, adbConnect;
    private boolean busy, alive, hasSnapshot;
    private String stockHome = "";
    private final Set<String> selected = new HashSet<>();
    private final Set<String> pendingPackages = new HashSet<>();

    private static final class Row {
        String pkg, name;
        long kb;
        List<MemorySnapshot.Entry> processes;
        boolean protectedApp;
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t;
    }
    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(12));
        if (stroke != 0) bg.setStroke(dp(2), stroke); return bg;
    }
    private Button button(String title, Runnable action) { return TvUi.button(this, title, action); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); alive = true;
        TvUi.prepare(this);
        am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        ResolveInfo home = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
        if (home != null && home.activityInfo != null) stockHome = home.activityInfo.packageName;
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(40), dp(24), dp(40), dp(16)); root.setBackground(TvUi.backdrop());
        TextView title = text("Bellekteki uygulamalar", 27, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD); root.addView(title);
        summary = text("RAM ölçülüyor…", 14, 0xffc0cee5); root.addView(summary);
        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(0, dp(14), 0, dp(12)); root.addView(actions);
        refresh = button("Yenile", () -> load(null));
        closeSelected = button("Seçilenleri kapat (0)", () -> {
            ArrayList<Row> targets = new ArrayList<>();
            for (Row row : rows) if (!row.protectedApp && selected.contains(row.pkg)) targets.add(row);
            close(targets);
        });
        selectCached = button("Önbellektekileri seç", () -> {
            for (Row row : rows) if (!row.protectedApp && MemorySnapshot.allCached(row.processes)) selected.add(row.pkg);
            updateSelection();
        });
        clearSelected = button("Seçimi temizle", () -> { selected.clear(); updateSelection(); });
        adbConnect = button(adbReady() ? "ADB bağlı" : "ADB bağla", () -> askAdb());
        Button back = button("Geri", () -> finish());
        for (Button b : new Button[]{refresh, closeSelected, selectCached, clearSelected, adbConnect, back}) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
            p.setMargins(0, 0, dp(12), 0); actions.addView(b, p);
        }
        message = text("", 12, 0xffc2d1e9); message.setPadding(0, 0, 0, dp(8)); root.addView(message);
        list = new ListView(this); list.setDividerHeight(dp(5)); list.setCacheColorHint(Color.TRANSPARENT);
        list.setSelector(shape(0x227ea7ed, 0xff8fb5f1)); list.setDrawSelectorOnTop(true);
        adapter = new BaseAdapter() {
            public int getCount() { return rows.size(); }
            public Object getItem(int i) { return rows.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int index, View convert, ViewGroup parent) {
                Row row = rows.get(index);
                LinearLayout line = new LinearLayout(AppManagerActivity.this);
                line.setGravity(Gravity.CENTER_VERTICAL); line.setPadding(dp(16), dp(9), dp(16), dp(9));
                line.setBackground(shape(selected.contains(row.pkg) ? 0xff304c70 : TvUi.SURFACE, 0));
                TextView check = text(row.protectedApp ? "•" : selected.contains(row.pkg) ? "☑" : "☐", 24, 0xffb9d7ff);
                line.addView(check, new LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.WRAP_CONTENT));
                LinearLayout names = new LinearLayout(AppManagerActivity.this); names.setOrientation(LinearLayout.VERTICAL);
                TextView label = text(row.name + (row.protectedApp ? " · Korunuyor" : ""), 17, Color.WHITE);
                names.addView(label);
                String state = MemorySnapshot.allCached(row.processes) ? "Önbellekte" : "Arka plan / servis";
                if (row.pkg.equals(getPackageName())) state = "Bu ekran";
                names.addView(text(state + " · " + row.processes.size() + " işlem", 11, TvUi.MUTED));
                line.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                TextView value = text(row.kb > 0 ? String.format(Locale.US, "~%.1f MB", row.kb / 1024d) : "Ölçüm yok", 19, 0xffdce8ff);
                line.addView(value); line.setMinimumHeight(dp(61));
                line.setContentDescription(row.name + ", " + value.getText() + ", " + state + ", " +
                    (row.protectedApp ? "Korunuyor" : selected.contains(row.pkg) ? "Seçili" : "Seçili değil"));
                return line;
            }
        };
        list.setAdapter(adapter); list.setOnItemClickListener((p, v, i, id) -> {
            Row row = rows.get(i);
            if (row.protectedApp) { message.setText(row.name + " korunuyor; kapatma listesine eklenmez."); return; }
            if (!selected.remove(row.pkg)) selected.add(row.pkg);
            updateSelection();
        });
        list.setOnItemLongClickListener((p, v, i, id) -> { choices(rows.get(i)); return true; });
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        TextView note = text("OK: seç / kaldır  ·  Uzun OK: ayrıntılar  ·  RAM değerleri Android’in son ölçümüdür", 11, 0xffa4b6d2);
        note.setPadding(0, dp(8), 0, 0); root.addView(note); setContentView(root);
    }
    @Override public void onResume() { super.onResume(); load(null); }
    @Override public void onDestroy() { alive = false; ui.removeCallbacksAndMessages(null); super.onDestroy(); }
    private boolean protectedPackage(String pkg) {
        return pkg.equals(getPackageName()) || pkg.equals(stockHome) || pkg.equals("com.android.tv.settings") ||
            pkg.equals("io.github.jqssun.airplay") || pkg.equals("com.boost.airplay.receiver");
    }
    private void setBusy(boolean state) {
        busy = state; closeSelected.setEnabled(!state && !selected.isEmpty());
        selectCached.setEnabled(!state && hasSnapshot); clearSelected.setEnabled(!state && !selected.isEmpty());
        refresh.setEnabled(!state); adbConnect.setEnabled(!state); list.setEnabled(!state);
    }
    private void updateSelection() {
        closeSelected.setText("Seçilenleri kapat (" + selected.size() + ")");
        closeSelected.setEnabled(!busy && !selected.isEmpty());
        clearSelected.setEnabled(!busy && !selected.isEmpty());
        adapter.notifyDataSetChanged();
        message.setText(selected.isEmpty() ? "Kapatmak istediklerini OK ile işaretle." : selected.size() + " uygulama seçildi. Üstte Seçilenleri kapat’a bas.");
    }
    private String dump() throws Exception {
        if (adbReady()) return adb().memoryDump();
        if (checkSelfPermission(Manifest.permission.DUMP) != PackageManager.PERMISSION_GRANTED)
            throw new IllegalStateException("Bellek tanılama izni etkin değil.");
        if (checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) != PackageManager.PERMISSION_GRANTED)
            throw new IllegalStateException("Android’in istediği kullanım bilgisi okuma izni etkin değil.");
        // The Mi Stick does not expose meminfo_service to ordinary apps. The activity
        // diagnostic interface is supported with the same two approved read permissions.
        Process process = new ProcessBuilder("/system/bin/dumpsys", "-t", "8", "activity", "processes").redirectErrorStream(true).start();
        Runnable stop = () -> process.destroy(); ui.postDelayed(stop, 12000);
        try {
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (out.length() > 2000000) throw new IllegalStateException("Tanılama çıktısı çok büyük.");
                    out.append(line).append('\n');
                }
            }
            if (process.waitFor() != 0) throw new IllegalStateException("Bellek bilgisi okunamadı.");
            String result = out.toString();
            if (result.contains("Permission Denial")) {
                Log.e("HizliTV.Apps", result.substring(0, Math.min(result.length(), 800)));
                throw new IllegalStateException("Android bellek listesine erişim vermedi.");
            }
            return result;
        } finally { ui.removeCallbacks(stop); process.destroy(); }
    }
    private boolean adbReady() { return getSharedPreferences("local_adb", 0).getBoolean("authorized", false); }
    private LocalAdb adb() {
        return new LocalAdb(new File(getFilesDir(), "local-adb"), bytes -> Base64.encodeToString(bytes, Base64.NO_WRAP));
    }
    private void askAdb() {
        if (busy) return;
        new AlertDialog.Builder(this).setTitle("Cihaz içi ADB bağlantısı")
            .setMessage("Bu Mi Stick’te seçilen uygulamaları doğrudan durdurmak için ADB eşleştirmesi gerekir. " +
                "ADB daha geniş cihaz kontrolü sağlar. HızlıTV yalnızca bu cihaza bağlanır; listede seçtiklerini durdurur. " +
                "Root kullanılmaz. TV’de çıkan hata ayıklama penceresini kendin onaylamalısın. " +
                "Her bağlantıda izin sorulmaması için varsa ‘Bu bilgisayardan her zaman izin ver’ kutusunu işaretle.")
            .setPositiveButton("Bağlantıyı başlat", (d, w) -> connectAdb())
            .setNeutralButton("Bağlantıyı unut", (d, w) -> forgetAdb())
            .setNegativeButton("Vazgeç", null).show();
    }
    private void forgetAdb() {
        try {
            audit("local_adb_forget_request", Collections.emptyList(), 0, -1, 0);
            getSharedPreferences("local_adb", 0).edit().putBoolean("authorized", false).commit();
            File directory = new File(getFilesDir(), "local-adb");
            for (String name : new String[]{"adb-private.pk8", "adb-public.x509"}) {
                File file = new File(directory, name);
                if (file.exists() && !file.delete()) throw new IllegalStateException("Bağlantı kimliği silinemedi.");
            }
            audit("local_adb_forgotten", Collections.emptyList(), 0, -1, 0);
            adbConnect.setText("ADB bağla");
            rows.clear(); selected.clear(); hasSnapshot = false;
            adapter.notifyDataSetChanged(); updateSelection(); memorySummary();
            message.setText("HızlıTV bağlantı kimliği silindi. Yeniden kullanmak için TV’de yeni izin gerekir.");
        } catch (Exception error) { message.setText("Bağlantı unutulamadı: " + error.getMessage()); }
    }
    private void connectAdb() {
        setBusy(true); message.setText("Yerel bağlantı kuruluyor; TV’de izin penceresi çıkabilir.");
        new Thread(() -> {
            try {
                audit("local_adb_pair_request", Collections.emptyList(), 0, -1, 0);
                adb().authorize(() -> {
                    Log.i("HizliTV.Adb", "TV RSA authorization requested for HizliTV@MiStick");
                    ui.post(() -> { if (alive) message.setText("TV’deki USB hata ayıklama penceresinde HızlıTV için izin ver."); });
                });
                // A one-time approval only authorizes that socket. Prove a fresh signed
                // connection works before presenting the connection as ready.
                if (MemorySnapshot.parse(adb().memoryDump()).entries.isEmpty())
                    throw new IllegalStateException("Yeni bağlantıda süreç listesi doğrulanamadı.");
                audit("local_adb_authorized_shell", Collections.emptyList(), 0, -1, 0);
                getSharedPreferences("local_adb", 0).edit().putBoolean("authorized", true).apply();
                ui.post(() -> { if (alive) { setBusy(false); adbConnect.setText("ADB bağlı");
                    load(() -> message.setText("Bağlantı hazır. Uygulamaları işaretleyip Seçilenleri kapat’a bas.")); } });
            } catch (Exception error) {
                Log.e("HizliTV.Adb", "Local connection", error);
                getSharedPreferences("local_adb", 0).edit().putBoolean("authorized", false).apply();
                ui.post(() -> { if (alive) { setBusy(false); adbConnect.setText("ADB bağla"); message.setText("Bağlantı kalıcı doğrulanamadı. TV’de ‘Her zaman izin ver’i seçip yeniden bağlan."); } });
            }
        }, "LocalAdbPair").start();
    }
    private ArrayList<Row> readRows() throws Exception {
        MemorySnapshot snapshot = MemorySnapshot.parse(dump());
        if (snapshot.entries.isEmpty()) throw new IllegalStateException("Süreç listesi okunamadı.");
        Set<String> packages = new HashSet<>();
        for (ResolveInfo info : getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)) packages.add(info.activityInfo.packageName);
        packages.add(stockHome); packages.add(getPackageName());
        ArrayList<Row> found = new ArrayList<>();
        for (String pkg : packages) {
            List<MemorySnapshot.Entry> entries = snapshot.forPackage(pkg);
            if (entries.isEmpty()) continue;
            try {
                ApplicationInfo info = getPackageManager().getApplicationInfo(pkg, 0);
                Row row = new Row(); row.pkg = pkg; row.name = info.loadLabel(getPackageManager()).toString();
                row.processes = entries; row.protectedApp = protectedPackage(pkg);
                for (MemorySnapshot.Entry entry : entries) row.kb += entry.pssKb;
                found.add(row);
            } catch (PackageManager.NameNotFoundException ignored) { }
        }
        Collections.sort(found, (a, b) -> Long.compare(b.kb, a.kb));
        return found;
    }
    private void memorySummary() {
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo(); am.getMemoryInfo(info);
        summary.setText("Kullanılabilir RAM: " + info.availMem / 1048576 + " MB  ·  Sistem bellek durumu: " +
            (info.lowMemory ? "Az bellek" : "Normal") + "  ·  " +
            (hasSnapshot ? rows.size() + " TV uygulaması bellekte" : "Uygulama listesi henüz okunamadı"));
    }
    private void audit(String event, List<Row> targets, long before, long after, int errors) throws Exception {
        JSONObject item = new JSONObject(); item.put("time_ms", System.currentTimeMillis()); item.put("event", event);
        JSONArray packages = new JSONArray(); for (Row row : targets) packages.put(row.pkg);
        item.put("requested_packages", packages); item.put("before_available_bytes", before);
        if (after >= 0) item.put("after_available_bytes", after);
        item.put("errors", errors);
        item.put("undo", event.startsWith("local_adb")
            ? "Bağlantıyı unut: HızlıTV kimliğini siler. Yeniden bağlanmak TV’de yeni izin gerektirir."
            : "Uygulamayı yeniden aç; uygulama verisi silinmedi.");
        File directory = getExternalFilesDir(null);
        if (directory == null) throw new IllegalStateException("İşlem kaydı oluşturulamadı.");
        try (FileWriter writer = new FileWriter(new File(directory, "memory-operations.jsonl"), true)) {
            writer.write(item.toString() + "\n");
        }
        Log.i("HizliTV.Memory", item.toString());
    }
    private void load(Runnable after) {
        if (busy) return; setBusy(true); message.setText("Bellekteki uygulamalar okunuyor…");
        new Thread(() -> {
            try {
                ArrayList<Row> found = readRows();
                ui.post(() -> {
                    if (!alive) return;
                    rows.clear(); rows.addAll(found); hasSnapshot = true;
                    Set<String> present = new HashSet<>(); for (Row row : rows) if (!row.protectedApp) present.add(row.pkg);
                    selected.retainAll(present); memorySummary(); setBusy(false); updateSelection();
                    if (!pendingPackages.isEmpty()) {
                        int remains = 0; for (Row row : rows) if (pendingPackages.contains(row.pkg)) remains++;
                        message.setText((pendingPackages.size() - remains) + " uygulama artık bellekte görünmüyor" +
                            (remains > 0 ? "; " + remains + " uygulama bellekte kaldı veya yeniden başladı." : "."));
                        pendingPackages.clear();
                    }
                    if (after != null) after.run();
                });
            } catch (Exception error) {
                Log.e("HizliTV.Apps", "Memory list", error);
                ui.post(() -> { if (alive) { rows.clear(); selected.clear(); hasSnapshot = false; updateSelection(); setBusy(false);
                    memorySummary(); message.setText(error.getMessage()); } });
            }
        }, "AppMemoryList").start();
    }
    private void choices(Row row) {
        if (busy) return;
        ArrayList<String> labels = new ArrayList<>();
        if (!row.protectedApp) labels.add("Arka plan işlemini kapat");
        labels.add("Uygulama ayrıntıları / Zorla durdur"); labels.add("Uygulamayı aç");
        new AlertDialog.Builder(this).setTitle(row.name + " · " + String.format(Locale.US, "%.1f MB", row.kb / 1024d))
            .setItems(labels.toArray(new String[0]), (dialog, index) -> {
                String choice = labels.get(index);
                if (choice.equals("Arka plan işlemini kapat")) close(Collections.singletonList(row));
                else if (choice.startsWith("Uygulama ayrıntıları")) {
                    try {
                        audit("open_app_details", Collections.singletonList(row), 0, -1, 0);
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + row.pkg)));
                    } catch (Exception e) { message.setText("Uygulama ayrıntıları açılamadı."); }
                } else {
                    Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(row.pkg);
                    if (intent != null) startActivity(intent);
                }
            }).setNegativeButton("Geri", null).show();
    }
    private void close(List<Row> targets) {
        if (busy) return;
        if (targets.isEmpty()) { message.setText("Önce kapatmak istediğin uygulamaları işaretle."); return; }
        for (Row row : targets) if (row.protectedApp) { message.setText("Bu uygulama korunuyor."); return; }
        if (!adbReady()) { askAdb(); return; }
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo(); am.getMemoryInfo(info);
        final long before = info.availMem;
        setBusy(true); message.setText("Seçilen arka plan işlemleri kapatılıyor…");
        new Thread(() -> {
            int errors = 0;
            try {
                audit("app_close_request", targets, before, -1, 0);
                for (Row row : targets) {
                    try { adb().stopPackage(row.pkg); } catch (Exception e) { errors++; Log.e("HizliTV.Adb", "Selected stop: " + row.pkg, e); }
                }
                Thread.sleep(1200);
                ActivityManager.MemoryInfo after = new ActivityManager.MemoryInfo(); am.getMemoryInfo(after);
                audit("app_close_result", targets, before, after.availMem, errors);
                final long available = after.availMem;
                ui.post(() -> {
                    if (!alive) return; setBusy(false);
                    pendingPackages.clear();
                    for (Row row : targets) { pendingPackages.add(row.pkg); selected.remove(row.pkg); }
                    load(() -> message.setText(message.getText() + "  RAM: " + before / 1048576 + " → " + available / 1048576 + " MB"));
                });
            } catch (Exception error) {
                ui.post(() -> { if (alive) { setBusy(false); message.setText("İşlem tamamlanamadı: " + error.getMessage()); } });
            }
        }, "CloseBackgroundApp").start();
    }
}
