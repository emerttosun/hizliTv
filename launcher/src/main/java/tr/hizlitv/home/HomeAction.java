package tr.hizlitv.home;

/** Stable dashboard tools shown as the top icon row, independent of view order. */
enum HomeAction {
    AIRPLAY(tr.hizlitv.ui.R.string.action_airplay), PROCESSES(tr.hizlitv.ui.R.string.home_memory),
    SETTINGS(tr.hizlitv.ui.R.string.text_settings), STOCK_HOME(tr.hizlitv.ui.R.string.text_stock_home);
    final int title;
    HomeAction(int title) { this.title = title; }
}
