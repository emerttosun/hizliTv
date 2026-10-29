package tr.hizlitv.home;

import tr.hizlitv.ui.R;
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
public final class AppManagerActivity extends tr.hizlitv.ui.LocalizedActivity {
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
        TextView title = text(getString(R.string.text_apps_in_memory), 27, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD); root.addView(title);
        summary = text(getString(R.string.text_measuring_ram), 14, 0xffc0cee5); root.addView(summary);
        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(0, dp(14), 0, dp(12)); root.addView(actions);
        refresh = button(getString(R.string.text_refresh), () -> load(null));
        closeSelected = button(getString(R.string.text_close_selected_0), () -> {
            ArrayList<Row> targets = new ArrayList<>();
            for (Row row : rows) if (!row.protectedApp && selected.contains(row.pkg)) targets.add(row);
            close(targets);
        });
        selectCached = button(getString(R.string.text_select_cached), () -> {
            for (Row row : rows) if (!row.protectedApp && MemorySnapshot.allCached(row.processes)) selected.add(row.pkg);
            updateSelection();
        });
        clearSelected = button(getString(R.string.text_clear_selection), () -> { selected.clear(); updateSelection(); });
        adbConnect = button(adbReady() ? getString(R.string.text_adb_connected) : getString(R.string.text_connect_adb), () -> askAdb());
        Button back = button(getString(R.string.text_back), () -> finish());
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
                TextView label = text(row.name + (row.protectedApp ? getString(R.string.text_protected) : ""), 17, Color.WHITE);
                names.addView(label);
                String state = MemorySnapshot.allCached(row.processes) ? getString(R.string.text_cached) : getString(R.string.text_background_service);
                if (row.pkg.equals(getPackageName())) state = getString(R.string.text_this_screen);
                names.addView(text(state + " · " + row.processes.size() + getString(R.string.text_processes), 11, TvUi.MUTED));
                line.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                TextView value = text(row.kb > 0 ? String.format(Locale.US, "~%.1f MB", row.kb / 1024d) : getString(R.string.text_not_measured), 19, 0xffdce8ff);
                line.addView(value); line.setMinimumHeight(dp(61));
                line.setContentDescription(row.name + ", " + value.getText() + ", " + state + ", " +
                    (row.protectedApp ? getString(R.string.text_protected_0904d0) : selected.contains(row.pkg) ? getString(R.string.text_selected) : getString(R.string.text_not_selected)));
                return line;
            }
        };
        list.setAdapter(adapter); list.setOnItemClickListener((p, v, i, id) -> {
            Row row = rows.get(i);
            if (row.protectedApp) { message.setText(row.name + getString(R.string.text_is_protected_and_cannot_be_selected_for_closure)); return; }
            if (!selected.remove(row.pkg)) selected.add(row.pkg);
            updateSelection();
        });
        list.setOnItemLongClickListener((p, v, i, id) -> { choices(rows.get(i)); return true; });
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        TextView note = text(getString(R.string.text_ok_select_clear_hold_ok_details_ram_values_are_android_s_l), 11, 0xffa4b6d2);
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
        closeSelected.setText(getString(R.string.text_close_selected) + selected.size() + ")");
        closeSelected.setEnabled(!busy && !selected.isEmpty());
        clearSelected.setEnabled(!busy && !selected.isEmpty());
        adapter.notifyDataSetChanged();
        message.setText(selected.isEmpty() ? getString(R.string.text_select_the_apps_to_close_with_ok) : selected.size() + getString(R.string.text_apps_selected_press_close_selected_above));
    }
    private String dump() throws Exception {
        if (adbReady()) return adb().memoryDump();
        if (checkSelfPermission(Manifest.permission.DUMP) != PackageManager.PERMISSION_GRANTED)
            throw new IllegalStateException(getString(R.string.text_memory_diagnostics_permission_is_not_enabled));
        if (checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) != PackageManager.PERMISSION_GRANTED)
            throw new IllegalStateException(getString(R.string.text_the_usage_access_permission_required_by_android_is_not_ena));
        // The Mi Stick does not expose meminfo_service to ordinary apps. The activity
        // diagnostic interface is supported with the same two approved read permissions.
        Process process = new ProcessBuilder("/system/bin/dumpsys", "-t", "8", "activity", "processes").redirectErrorStream(true).start();
        Runnable stop = () -> process.destroy(); ui.postDelayed(stop, 12000);
        try {
            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (out.length() > 2000000) throw new IllegalStateException(getString(R.string.text_diagnostics_output_is_too_large));
                    out.append(line).append('\n');
                }
            }
            if (process.waitFor() != 0) throw new IllegalStateException(getString(R.string.text_memory_information_could_not_be_read));
            String result = out.toString();
            if (result.contains("Permission Denial")) {
                Log.e("HizliTV.Apps", result.substring(0, Math.min(result.length(), 800)));
                throw new IllegalStateException(getString(R.string.text_android_did_not_allow_access_to_the_memory_list));
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
        new AlertDialog.Builder(this).setTitle(getString(R.string.text_on_device_adb_connection))
            .setMessage(getString(R.string.text_closing_selected_apps_directly_on_this_device_requires_adb) +
                getString(R.string.text_adb_provides_broader_device_control_h_zl_tv_connects_only) +
                getString(R.string.text_no_root_is_used_you_must_approve_the_debugging_request_on) +
                getString(R.string.text_if_available_select_always_allow_from_this_computer_to_avo))
            .setPositiveButton(getString(R.string.text_start_connection), (d, w) -> connectAdb())
            .setNeutralButton(getString(R.string.text_forget_connection), (d, w) -> forgetAdb())
            .setNegativeButton(getString(R.string.text_cancel), null).show();
    }
    private void forgetAdb() {
        try {
            audit("local_adb_forget_request", Collections.emptyList(), 0, -1, 0);
            getSharedPreferences("local_adb", 0).edit().putBoolean("authorized", false).commit();
            File directory = new File(getFilesDir(), "local-adb");
            for (String name : new String[]{"adb-private.pk8", "adb-public.x509"}) {
                File file = new File(directory, name);
                if (file.exists() && !file.delete()) throw new IllegalStateException(getString(R.string.text_connection_identity_could_not_be_deleted));
            }
            audit("local_adb_forgotten", Collections.emptyList(), 0, -1, 0);
            adbConnect.setText(getString(R.string.text_connect_adb));
            rows.clear(); selected.clear(); hasSnapshot = false;
            adapter.notifyDataSetChanged(); updateSelection(); memorySummary();
            message.setText(getString(R.string.text_h_zl_tv_s_connection_identity_was_deleted_a_new_approval_o));
        } catch (Exception error) { message.setText(getString(R.string.text_could_not_forget_the_connection) + tr.hizlitv.ui.I18n.error(this, error)); }
    }
    private void connectAdb() {
        setBusy(true); message.setText(getString(R.string.text_connecting_locally_the_tv_may_ask_for_permission));
        new Thread(() -> {
            try {
                audit("local_adb_pair_request", Collections.emptyList(), 0, -1, 0);
                adb().authorize(() -> {
                    Log.i("HizliTV.Adb", "TV RSA authorization requested for HizliTV@MiStick");
                    ui.post(() -> { if (alive) message.setText(getString(R.string.text_allow_h_zl_tv_in_the_tv_s_usb_debugging_dialog)); });
                });
                // A one-time approval only authorizes that socket. Prove a fresh signed
                // connection works before presenting the connection as ready.
                if (MemorySnapshot.parse(adb().memoryDump()).entries.isEmpty())
                    throw new IllegalStateException(getString(R.string.text_the_process_list_could_not_be_verified_after_reconnecting));
                audit("local_adb_authorized_shell", Collections.emptyList(), 0, -1, 0);
                getSharedPreferences("local_adb", 0).edit().putBoolean("authorized", true).apply();
                ui.post(() -> { if (alive) { setBusy(false); adbConnect.setText(getString(R.string.text_adb_connected));
                    load(() -> message.setText(getString(R.string.text_connection_ready_select_apps_and_press_close_selected))); } });
            } catch (Exception error) {
                Log.e("HizliTV.Adb", "Local connection", error);
                getSharedPreferences("local_adb", 0).edit().putBoolean("authorized", false).apply();
                ui.post(() -> { if (alive) { setBusy(false); adbConnect.setText(getString(R.string.text_connect_adb)); message.setText(getString(R.string.text_the_connection_could_not_be_verified_persistently_select_a)); } });
            }
        }, "LocalAdbPair").start();
    }
    private ArrayList<Row> readRows() throws Exception {
        MemorySnapshot snapshot = MemorySnapshot.parse(dump());
        if (snapshot.entries.isEmpty()) throw new IllegalStateException(getString(R.string.text_the_process_list_could_not_be_read));
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
        summary.setText(getString(R.string.text_available_ram) + info.availMem / 1048576 + getString(R.string.text_mb_system_memory_state) +
            (info.lowMemory ? getString(R.string.low_memory) : "Normal") + "  ·  " +
            (hasSnapshot ? rows.size() + getString(R.string.text_tv_apps_in_memory) : getString(R.string.text_the_app_list_has_not_been_read_yet)));
    }
    private void audit(String event, List<Row> targets, long before, long after, int errors) throws Exception {
        JSONObject item = new JSONObject(); item.put("time_ms", System.currentTimeMillis()); item.put("event", event);
        JSONArray packages = new JSONArray(); for (Row row : targets) packages.put(row.pkg);
        item.put("requested_packages", packages); item.put("before_available_bytes", before);
        if (after >= 0) item.put("after_available_bytes", after);
        item.put("errors", errors);
        item.put("undo", event.startsWith("local_adb")
            ? getString(R.string.text_forget_connection_removes_h_zl_tv_s_identity_reconnecting)
            : getString(R.string.text_reopen_the_app_its_data_was_not_deleted));
        File directory = getExternalFilesDir(null);
        if (directory == null) throw new IllegalStateException(getString(R.string.text_the_operation_log_could_not_be_created));
        try (FileWriter writer = new FileWriter(new File(directory, "memory-operations.jsonl"), true)) {
            writer.write(item.toString() + "\n");
        }
        Log.i("HizliTV.Memory", item.toString());
    }
    private void load(Runnable after) {
        if (busy) return; setBusy(true); message.setText(getString(R.string.text_reading_apps_in_memory));
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
                        message.setText((pendingPackages.size() - remains) + getString(R.string.text_apps_no_longer_appear_in_memory) +
                            (remains > 0 ? "; " + remains + getString(R.string.text_apps_remain_in_memory_or_have_restarted) : "."));
                        pendingPackages.clear();
                    }
                    if (after != null) after.run();
                });
            } catch (Exception error) {
                Log.e("HizliTV.Apps", "Memory list", error);
                ui.post(() -> { if (alive) { rows.clear(); selected.clear(); hasSnapshot = false; updateSelection(); setBusy(false);
                    memorySummary(); message.setText(tr.hizlitv.ui.I18n.error(this, error)); } });
            }
        }, "AppMemoryList").start();
    }
    private void choices(Row row) {
        if (busy) return;
        ArrayList<String> labels = new ArrayList<>();
        if (!row.protectedApp) labels.add(getString(R.string.text_close_background_process));
        labels.add(getString(R.string.text_app_details_force_stop)); labels.add(getString(R.string.text_open_app));
        new AlertDialog.Builder(this).setTitle(row.name + " · " + String.format(Locale.US, "%.1f MB", row.kb / 1024d))
            .setItems(labels.toArray(new String[0]), (dialog, index) -> {
                String choice = labels.get(index);
                if (choice.equals(getString(R.string.text_close_background_process))) close(Collections.singletonList(row));
                else if (choice.startsWith(getString(R.string.text_app_details))) {
                    try {
                        audit("open_app_details", Collections.singletonList(row), 0, -1, 0);
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + row.pkg)));
                    } catch (Exception e) { message.setText(getString(R.string.text_app_details_could_not_be_opened)); }
                } else {
                    Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(row.pkg);
                    if (intent != null) startActivity(intent);
                }
            }).setNegativeButton(getString(R.string.text_back), null).show();
    }
    private void close(List<Row> targets) {
        if (busy) return;
        if (targets.isEmpty()) { message.setText(getString(R.string.text_first_select_the_apps_you_want_to_close)); return; }
        for (Row row : targets) if (row.protectedApp) { message.setText(getString(R.string.text_this_app_is_protected)); return; }
        if (!adbReady()) { askAdb(); return; }
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo(); am.getMemoryInfo(info);
        final long before = info.availMem;
        setBusy(true); message.setText(getString(R.string.text_closing_selected_background_processes));
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
                ui.post(() -> { if (alive) { setBusy(false); message.setText(getString(R.string.text_the_operation_could_not_be_completed) + tr.hizlitv.ui.I18n.error(this, error)); } });
            }
        }, "CloseBackgroundApp").start();
    }
}
