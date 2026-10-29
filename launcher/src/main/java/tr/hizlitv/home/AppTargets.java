package tr.hizlitv.home;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

/** Optional integrations live here; installed applications are discovered at runtime. */
final class AppTargets {
    static final String PLAYER = "tr.hizlitv.player";
    static final String[] AIRPLAY = {"io.github.jqssun.airplay", "com.boost.airplay.receiver"};
    static final String[] DEFAULT_FAVORITES = {PLAYER, "com.google.android.youtube.tv", "com.netflix.ninja",
        "ar.tvplayer.tv", "com.disney.disneyplus", "com.amazon.amazonvideo.livingroom",
        "com.wbd.stream", "com.digiturk.iq.mobil", "com.turkcell.ott"};
    static Intent player(Context c) { return new Intent(c, tr.hizlitv.player.MainActivity.class); }
    static Intent installed(Context c, String... candidates) {
        PackageManager pm = c.getPackageManager();
        for (String pkg : candidates) {
            Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (intent == null) intent = pm.getLaunchIntentForPackage(pkg);
            if (intent != null) return intent;
        }
        return null;
    }
    static Intent stockHome(Context c) {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolved = c.getPackageManager().resolveActivity(home, 0);
        if (resolved != null && resolved.activityInfo != null && !resolved.activityInfo.packageName.equals(c.getPackageName()))
            return home.setClassName(resolved.activityInfo.packageName, resolved.activityInfo.name);
        return home;
    }
    static boolean protectedFromCleanup(String pkg) {
        if (PLAYER.equals(pkg)) return true;
        for (String candidate : AIRPLAY) if (candidate.equals(pkg)) return true;
        return false;
    }
}
