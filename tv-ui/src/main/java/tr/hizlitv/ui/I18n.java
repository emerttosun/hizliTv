package tr.hizlitv.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Localizes known domain errors at the UI boundary; core parsers remain plain Java. */
public final class I18n {
    private static volatile Map<String, Integer> messages;
    private I18n() { }
    private static Map<String, Integer> catalog(Context context) {
        if (messages == null) synchronized (I18n.class) {
            if (messages == null) {
                Configuration config = new Configuration(context.getResources().getConfiguration()); config.setLocale(Locale.ENGLISH);
                Context english = context.createConfigurationContext(config); Map<String, Integer> loaded = new LinkedHashMap<>();
                TypedArray ids = english.getResources().obtainTypedArray(R.array.translation_catalog);
                try { for (int i = 0; i < ids.length(); i++) { int id = ids.getResourceId(i, 0); if (id != 0) loaded.put(english.getString(id), id); } }
                finally { ids.recycle(); }
                messages = loaded;
            }
        }
        return messages;
    }
    public static String error(Context context, Throwable failure) {
        if (failure instanceof java.net.SocketTimeoutException) return context.getString(R.string.network_timeout);
        if (failure instanceof java.net.UnknownHostException || failure instanceof java.net.ConnectException) return context.getString(R.string.network_unavailable);
        return translate(context, failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage(), 0);
    }
    private static String translate(Context context, String text, int depth) {
        Map<String, Integer> all = catalog(context); Integer exact = all.get(text);
        if (exact != null) return context.getString(exact);
        String httpPrefix = "Source site returned HTTP ", httpSuffix = " response.";
        if (text.startsWith(httpPrefix) && text.endsWith(httpSuffix) && all.containsKey(httpPrefix) && all.containsKey(httpSuffix))
            return context.getString(all.get(httpPrefix)) + text.substring(httpPrefix.length(), text.length() - httpSuffix.length()) + context.getString(all.get(httpSuffix));
        if (depth < 3) for (Map.Entry<String, Integer> entry : all.entrySet()) {
            String prefix = entry.getKey();
            if (prefix.endsWith(": ") && text.startsWith(prefix))
                return context.getString(entry.getValue()) + translate(context, text.substring(prefix.length()), depth + 1);
        }
        return text;
    }
}
