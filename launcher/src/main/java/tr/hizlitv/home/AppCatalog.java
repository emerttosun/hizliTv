package tr.hizlitv.home;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import java.util.*;

/** Installed TV apps and favorites. Same preference keys preserve existing installations. */
final class AppCatalog {
    static final class Entry {
        final String name, pkg;
        final Intent intent;
        final Drawable artwork;
        final boolean banner;
        Entry(String name, String pkg, Intent intent, Drawable artwork, boolean banner) {
            this.name = name; this.pkg = pkg; this.intent = intent; this.artwork = artwork; this.banner = banner;
        }
    }
    private final Context context;
    private final SharedPreferences preferences;
    private final List<Entry> entries = new ArrayList<>(), favorites = new ArrayList<>();
    private Set<String> pinned;
    private boolean loaded, dirty = true;
    private long lastLoad;
    private int revision;
    void invalidate() { dirty = true; }
    int revision() { return revision; }
    AppCatalog(Context c) { context = c; preferences = c.getSharedPreferences("home", 0); }
    void refresh() {
        long now = android.os.SystemClock.elapsedRealtime();
        if (loaded && !dirty && now - lastLoad < 60_000) return;
        entries.clear(); favorites.clear();
        pinned = new HashSet<>(preferences.getStringSet("pins", new HashSet<>(Arrays.asList(AppTargets.DEFAULT_FAVORITES))));
        PackageManager pm = context.getPackageManager(); Set<String> seen = new HashSet<>();
        entries.add(new Entry("HızlıTV Player", AppTargets.PLAYER, AppTargets.player(context), null, false));
        seen.add(AppTargets.PLAYER);
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(context.getPackageName()) || !seen.add(pkg)) continue;
            Intent launch = new Intent(query).setComponent(new ComponentName(pkg, info.activityInfo.name))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            Drawable art = info.activityInfo.loadBanner(pm); boolean banner = art != null;
            if (art == null) art = info.loadIcon(pm);
            entries.add(new Entry(info.loadLabel(pm).toString(), pkg, launch, art, banner));
        }
        Collections.sort(entries, (a, b) -> a.name.compareToIgnoreCase(b.name));
        if (!preferences.getBoolean("player_pin_added", false)) {
            for (Entry entry : entries) if (entry.pkg.equals(AppTargets.PLAYER)) {
                pinned.add(entry.pkg); preferences.edit().putStringSet("pins", new HashSet<>(pinned))
                    .putBoolean("player_pin_added", true).apply(); break;
            }
        }
        rebuildFavorites(); loaded = true; dirty = false; lastLoad = now; revision++;
    }
    private void rebuildFavorites() {
        favorites.clear();
        for (String pkg : AppTargets.DEFAULT_FAVORITES)
            for (Entry entry : entries) if (pkg.equals(entry.pkg) && pinned.contains(pkg)) favorites.add(entry);
        for (Entry entry : entries) if (pinned.contains(entry.pkg) && !favorites.contains(entry)) favorites.add(entry);
    }
    List<Entry> entries(boolean all) { return Collections.unmodifiableList(all ? entries : favorites); }
    boolean pinned(Entry entry) { return pinned.contains(entry.pkg); }
    boolean toggle(Entry entry) {
        if (!pinned.remove(entry.pkg)) pinned.add(entry.pkg);
        preferences.edit().putStringSet("pins", new HashSet<>(pinned)).apply(); rebuildFavorites(); revision++; return pinned.contains(entry.pkg);
    }
}
