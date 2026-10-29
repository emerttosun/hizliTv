package tr.hizlitv.player;

import tr.hizlitv.media.MediaEntry;
import tr.hizlitv.media.M3u;
import tr.hizlitv.media.Urls;

import android.content.Context;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Private, atomically written library, with one-level undo for list edits. */
final class Store {
    private static final Object AUDIT_LOCK = new Object();
    private final Context context;
    private final AtomicFile file, undo;
    Store(Context context) {
        this.context = context;
        file = new AtomicFile(new File(context.getFilesDir(), "library.json"));
        undo = new AtomicFile(new File(context.getFilesDir(), "library-undo.json"));
    }
    static JSONObject json(MediaEntry e) throws Exception {
        JSONObject o = new JSONObject(); o.put("title", e.title); o.put("url", e.url); o.put("group", e.group);
        o.put("referer", e.referer); o.put("agent", e.userAgent); o.put("mime", e.mime); o.put("favorite", e.favorite);
        o.put("source_page", e.sourcePage);
        JSONArray subs = new JSONArray();
        for (MediaEntry.Subtitle s : e.subtitles) {
            JSONObject t = new JSONObject(); t.put("url", s.url); t.put("label", s.label); t.put("language", s.language); t.put("mime", s.mime); subs.put(t);
        }
        o.put("subtitles", subs); return o;
    }
    static MediaEntry entry(JSONObject o) throws Exception {
        MediaEntry e = new MediaEntry(); e.url = Urls.http(o.getString("url")); e.title = o.optString("title", "Video");
        e.group = o.optString("group"); e.referer = o.optString("referer"); e.userAgent = o.optString("agent");
        e.mime = o.optString("mime"); e.favorite = o.optBoolean("favorite");
        e.sourcePage = o.optString("source_page");
        JSONArray subs = o.optJSONArray("subtitles");
        if (subs != null) for (int i = 0; i < subs.length() && i < 50; i++) {
            JSONObject t = subs.getJSONObject(i);
            e.subtitles.add(new MediaEntry.Subtitle(Urls.http(t.getString("url")), t.optString("label"), t.optString("language"), t.optString("mime", "text/vtt")));
        }
        return e;
    }
    List<MediaEntry> read() throws Exception {
        ArrayList<MediaEntry> out = new ArrayList<>();
        if (!file.getBaseFile().exists()) return out;
        byte[] bytes = file.readFully(); if (bytes.length > 8_000_000) throw new java.io.IOException("Kayıt boyutu sınırı aşıldı.");
        JSONArray array = new JSONArray(new String(bytes, StandardCharsets.UTF_8));
        for (int i = 0; i < array.length() && i < 5000; i++) out.add(entry(array.getJSONObject(i)));
        return out;
    }
    void write(List<MediaEntry> items, String action) throws Exception {
        JSONArray array = new JSONArray(); for (MediaEntry e : items) array.put(json(e));
        byte[] current = array.toString().getBytes(StandardCharsets.UTF_8);
        if (current.length > 8_000_000) throw new java.io.IOException("Kütüphane boyutu sınırı aşıldı; mevcut kayıtlar korundu.");
        byte[] previous = file.getBaseFile().exists() ? file.readFully() : "[]".getBytes(StandardCharsets.UTF_8);
        atomic(undo, previous);
        atomic(file, current);
        audit(action, items.size());
    }
    void merge(List<MediaEntry> incoming) throws Exception {
        LinkedHashMap<String,MediaEntry> all = new LinkedHashMap<>();
        for (MediaEntry e : read()) all.put(identity(e), e);
        for (MediaEntry e : incoming) {
            MediaEntry previous = all.get(identity(e)); if (previous != null) e.favorite = previous.favorite || e.favorite;
            all.put(identity(e), e);
            if (all.size() > 5000) throw new java.io.IOException("Kütüphane en fazla 5000 kayıt tutabilir.");
        }
        write(new ArrayList<>(all.values()), "library_import");
    }
    static String identity(MediaEntry e) { return e.sourcePage.isEmpty() ? "url:" + e.url : "page:" + e.sourcePage; }
    void restore() throws Exception {
        if (!undo.getBaseFile().exists()) throw new java.io.IOException("Geri alınacak kütüphane değişikliği yok.");
        atomic(file, undo.readFully()); undo.delete(); audit("library_undo", read().size());
    }
    void audit(String action, int count) throws Exception {
        JSONObject event = new JSONObject(); event.put("time_ms", System.currentTimeMillis());
        event.put("action", action); event.put("count", count);
        event.put("undo", "Kütüphane değişikliği: Son değişikliği geri al. Oynatma: Geri tuşu.");
        synchronized (AUDIT_LOCK) { try (java.io.FileWriter out = new java.io.FileWriter(new File(context.getFilesDir(), "operations.jsonl"), true)) {
            out.write(event.toString()); out.write('\n');
        } }
        android.util.Log.i("HizliTV.Player", event.toString());
    }
    private static void atomic(AtomicFile file, byte[] bytes) throws Exception {
        FileOutputStream out = file.startWrite();
        try { out.write(bytes); file.finishWrite(out); } catch (Exception e) { file.failWrite(out); throw e; }
    }
}
