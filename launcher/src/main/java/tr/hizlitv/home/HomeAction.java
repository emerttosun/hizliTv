package tr.hizlitv.home;

/** Stable user-facing actions, independent of view order and package discovery. */
enum HomeAction {
    PLAYER(tr.hizlitv.ui.R.string.action_player, 92), APPS(tr.hizlitv.ui.R.string.text_all_apps, 134), AIRPLAY(tr.hizlitv.ui.R.string.action_airplay, 94),
    PROCESSES(tr.hizlitv.ui.R.string.text_apps_in_memory_90966c, 126), CLEAN(tr.hizlitv.ui.R.string.text_free_memory, 138), SETTINGS(tr.hizlitv.ui.R.string.text_settings, 96),
    STOCK_HOME(tr.hizlitv.ui.R.string.text_stock_home, 138);
    final int title;
    final int weight;
    HomeAction(int title, int weight) { this.title = title; this.weight = weight; }
}
