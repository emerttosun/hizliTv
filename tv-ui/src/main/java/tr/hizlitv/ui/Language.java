package tr.hizlitv.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import java.util.Locale;

/** English fallback, Turkish resources, and an optional app-only preference. */
public final class Language {
    private Language() { }
    @SuppressWarnings("deprecation")
    public static Locale locale(Context context) { return context.getResources().getConfiguration().locale; }
    public static String code(Context context) { return locale(context).getLanguage().equals("tr") ? "tr" : "en"; }
    public static Context wrap(Context base) {
        String choice = base.getSharedPreferences("appearance", 0).getString("language", "");
        String language = choice.isEmpty() ? locale(base).getLanguage() : choice;
        Locale selected = new Locale(language.equals("tr") ? "tr" : "en");
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(selected); config.setLayoutDirection(selected);
        return base.createConfigurationContext(config);
    }
    public static void show(Activity activity) {
        String[] labels = {activity.getString(R.string.language_system), activity.getString(R.string.language_english), activity.getString(R.string.language_turkish)};
        new TvDialog(activity, "HızlıTV", activity.getString(R.string.language), "")
            .choices(labels, index -> {
                activity.getSharedPreferences("appearance", 0).edit().putString("language", new String[]{"", "en", "tr"}[index]).apply();
                activity.recreate();
            }).action(activity.getString(R.string.text_cancel), null).show();
    }
}
