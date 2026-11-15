package tr.hizlitv.home;

import tr.hizlitv.ui.R;
import android.app.Activity;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.Toast;
import tr.hizlitv.ui.TvUi;

/** Coordinates the screen, installed apps and optional device tools. Never claims HOME. */
public final class MainActivity extends tr.hizlitv.ui.LocalizedActivity implements HomeScreen.Actions {
    private AppCatalog catalog;
    private HomeScreen screen;
    private MemoryController memory;
    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) { catalog.invalidate(); }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); TvUi.prepare(this); catalog = new AppCatalog(this);
        IntentFilter changes = new IntentFilter();
        changes.addAction(Intent.ACTION_PACKAGE_ADDED); changes.addAction(Intent.ACTION_PACKAGE_REMOVED); changes.addAction(Intent.ACTION_PACKAGE_CHANGED); changes.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(packages, changes, Context.RECEIVER_EXPORTED);
        else registerReceiver(packages, changes);
        screen = new HomeScreen(this, this); setContentView(screen);
        memory = new MemoryController(this, (used, available, result) -> screen.memory(used, available, result));
    }
    @Override public void onResume() {
        super.onResume(); catalog.refresh(); screen.show(catalog);
        screen.tool(HomeAction.AIRPLAY, AppTargets.installed(this, AppTargets.AIRPLAY) != null); memory.start(); screen.startClock();
    }
    @Override public void onPause() { memory.stop(); screen.stopClock(); super.onPause(); }
    @Override public void onDestroy() { unregisterReceiver(packages); memory.close(); super.onDestroy(); }
    @Override public void language() { tr.hizlitv.ui.Language.show(this); }
    @Override public void open(AppCatalog.Entry app) { launch(app.intent); }
    @Override public void pin(AppCatalog.Entry app) {
        boolean pinned = catalog.toggle(app);
        Toast.makeText(this, pinned ? getString(R.string.text_added_to_favorites) : getString(R.string.text_removed_from_favorites), Toast.LENGTH_SHORT).show();
        screen.show(catalog);
    }
    private void launch(Intent intent) {
        try { if (intent == null) throw new IllegalStateException(); startActivity(intent); }
        catch (Exception failure) { Toast.makeText(this, getString(R.string.text_app_not_found_or_could_not_be_opened), Toast.LENGTH_SHORT).show(); }
    }
    @Override public void action(HomeAction action) {
        switch (action) {
            case AIRPLAY: launch(AppTargets.installed(this, AppTargets.AIRPLAY)); break;
            case PROCESSES: launch(new Intent(this, AppManagerActivity.class)); break;
            case SETTINGS: launch(new Intent(Settings.ACTION_SETTINGS)); break;
            case STOCK_HOME: launch(AppTargets.stockHome(this)); break;
        }
    }
    @Override public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_MENU) { if (event.getRepeatCount() == 0) screen.jump(); return true; }
        return super.onKeyDown(key, event);
    }
    @Override public void onBackPressed() { if (!screen.back()) super.onBackPressed(); }
}
