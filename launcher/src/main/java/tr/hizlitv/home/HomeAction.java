package tr.hizlitv.home;

/** Stable user-facing actions, independent of view order and package discovery. */
enum HomeAction {
    PLAYER("Player", 92), APPS("Tüm uygulamalar", 134), AIRPLAY("AirPlay", 94),
    PROCESSES("Bellektekiler", 126), CLEAN("RAM’i rahatlat", 138), SETTINGS("Ayarlar", 96),
    STOCK_HOME("Eski ana ekran", 138);
    final String title;
    final int weight;
    HomeAction(String title, int weight) { this.title = title; this.weight = weight; }
}
