package tr.hizlitv.player;

import tr.hizlitv.media.MediaEntry;
import tr.hizlitv.media.M3u;
import tr.hizlitv.media.Urls;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.json.JSONArray;
import org.json.JSONObject;

/** Bounded HTTP/HTML resolver. Never executes page scripts or opens advertising links. */
final class Resolver {
    static final String AGENT = "HizliTVPlayer/0.1 (Android TV)";
    static final class Result {
        final List<MediaEntry> items = new ArrayList<>();
        boolean playlist;
        String warning = "";
    }
    private static final class Response { String url, type, body; boolean media; }
    private final Set<String> visited = new HashSet<>();
    private final LinkedHashMap<String,MediaEntry> found = new LinkedHashMap<>();
    private long deadline;
    private String sourcePage = "";
    private String frameError = "";
    private final List<SourceAdapter> adapters;
    Resolver() { this(SourceAdapters.defaults()); }
    Resolver(List<SourceAdapter> adapters) { this.adapters = new ArrayList<>(adapters); }
    private void note(String message) { android.util.Log.i("HizliTV.Resolve", message); }
    private void checkTime() throws Exception {
        if (Thread.currentThread().isInterrupted() || System.nanoTime() > deadline) throw new java.io.IOException("Kaynak arama süresi doldu.");
    }
    Result resolve(String input) throws Exception {
        deadline = System.nanoTime() + 30_000_000_000L;
        Result result = new Result(); String text = input.trim();
        if (text.startsWith("#EXTM3U") || text.startsWith("#EXTINF:")) {
            result.items.addAll(M3u.parse(text, "")); result.playlist = true; return result;
        }
        String url = Urls.http(text);
        if (Urls.media(url) && !Urls.mime(url).equals("application/x-mpegURL")) { add(url, "Video · " + Urls.host(url), "", ""); }
        else {
            Response response = fetch(url, "");
            if (response.media) add(response.url, "Video · " + Urls.host(response.url), "", response.type);
            else if (response.body.trim().startsWith("#EXTM3U")) {
                if (M3u.hls(response.body)) add(response.url, "Canlı yayın / video", "", "application/x-mpegURL");
                else { result.items.addAll(M3u.parse(response.body, response.url)); result.playlist = true; return result; }
            } else { sourcePage = response.url; scan(response, 0, ""); }
        }
        result.items.addAll(found.values());
        if (result.items.isEmpty()) throw new java.io.IOException(frameError.isEmpty()
            ? "Oynatılabilir kaynak bulunamadı. Sayfa giriş, farklı bir oynatıcı veya desteklenmeyen JavaScript gerektiriyor olabilir."
            : "Sayfadaki video kaynağı okunamadı: " + frameError);
        return result;
    }
    private Response fetch(String source, String referer) throws Exception {
        String url = source;
        for (int redirects = 0; redirects < 6; redirects++) {
            checkTime(); HttpURLConnection c = (HttpURLConnection)new URL(Urls.http(url)).openConnection();
            c.setConnectTimeout(7000); c.setReadTimeout(7000); c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent", AGENT);
            c.setRequestProperty("Accept-Encoding", "identity");
            if (!referer.isEmpty()) c.setRequestProperty("Referer", M3u.safeHeader(referer));
            try {
                int status = c.getResponseCode();
                note("HTTP host=" + Urls.host(url) + " status=" + status);
                if (status >= 300 && status < 400) { url = Urls.resolve(url, c.getHeaderField("Location")); continue; }
                if (status != 200 && status != 206) throw new java.io.IOException("Kaynak sitesi HTTP " + status + " yanıtı verdi.");
                Response r = new Response(); r.url = url;
                r.type = c.getContentType() == null ? "" : c.getContentType().split(";")[0].trim().toLowerCase(java.util.Locale.ROOT);
                r.media = r.type.startsWith("video/") || r.type.equals("application/dash+xml");
                if (r.media) { r.body = ""; return r; }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (InputStream in = c.getInputStream()) {
                    byte[] buffer = new byte[8192]; int n;
                    while ((n = in.read(buffer)) != -1) {
                        checkTime(); if (out.size() + n > 2_000_000) throw new java.io.IOException("Sayfa/liste 2 MB sınırını aşıyor.");
                        out.write(buffer, 0, n);
                    }
                }
                r.body = new String(out.toByteArray(), StandardCharsets.UTF_8); return r;
            } finally { c.disconnect(); }
        }
        throw new java.io.IOException("Çok fazla yönlendirme var.");
    }
    private MediaEntry add(String url, String title, String referer, String mime) {
        url = Urls.http(url);
        MediaEntry e = found.get(url);
        if (e == null && found.size() < 30) {
            e = new MediaEntry(); e.url = url; e.title = title.isEmpty() ? "Video" : M3u.shorten(title, 256);
            e.referer = referer; e.mime = mime.isEmpty() ? Urls.mime(url) : mime;
            e.sourcePage = sourcePage;
            found.put(url, e);
        }
        return e;
    }
    private void scan(Response r, int depth, String parentTitle) throws Exception {
        if (depth > 2 || visited.size() >= 6 || !visited.add(r.url)) return;
        checkTime();
        Document doc = Jsoup.parse(r.body, r.url);
        note("HTML host=" + Urls.host(r.url) + " bytes=" + r.body.length() + " frames=" + doc.select("iframe[src], iframe[data-src]").size());
        String title = parentTitle.isEmpty() ? doc.title() : parentTitle;
        if (title.isEmpty()) title = "Video";
        ArrayList<MediaEntry> pageItems = new ArrayList<>();
        for (Element el : doc.select("video[src], source[src], meta[property=og:video], meta[property=og:video:url], meta[property=og:video:secure_url]")) {
            String value = el.hasAttr("src") ? el.attr("src") : el.attr("content");
            try { MediaEntry e = add(Urls.resolve(r.url, value), title, r.url, el.attr("type")); if (e != null) pageItems.add(e); }
            catch (IllegalArgumentException ignored) { }
        }
        // This adapter reads the site's URL transform only. It does not evaluate script.
        for (SourceAdapter adapter : adapters) if (adapter.supports(Urls.host(r.url))) {
            String decoded = adapter.extract(r.body, Urls.host(r.url));
            note("Static adapter host=" + Urls.host(r.url) + " found=" + !decoded.isEmpty());
            if (!decoded.isEmpty()) { MediaEntry e = add(decoded, title, r.url, "application/x-mpegURL"); if (e != null) pageItems.add(e); }
        }
        Matcher direct = Pattern.compile("[\"']((?:https?:)?(?:\\\\/|/){2}[^\"'<>\\s]+?\\.(?:m3u8|mp4|mpd)(?:\\?[^\"'<>\\s]*)?)[\"']", Pattern.CASE_INSENSITIVE).matcher(r.body);
        while (direct.find() && found.size() < 30) {
            try { MediaEntry e = add(Urls.resolve(r.url, org.jsoup.parser.Parser.unescapeEntities(direct.group(1), true)), title, r.url, ""); if (e != null && !pageItems.contains(e)) pageItems.add(e); }
            catch (IllegalArgumentException ignored) { }
        }
        for (Element track : doc.select("track[src]")) {
            String kind = track.attr("kind");
            if (kind.isEmpty() || kind.equals("subtitles") || kind.equals("captions"))
                for (MediaEntry e : pageItems) subtitle(e, r.url, track.attr("src"), track.attr("label"), track.attr("srclang"));
        }
        Matcher tracks = Pattern.compile("\\btracks\\s*:\\s*(\\[[^\\]]{0,32000}\\])").matcher(r.body);
        if (tracks.find()) {
            try {
                JSONArray array = new JSONArray(tracks.group(1));
                for (int i = 0; i < array.length() && i < 50; i++) {
                    JSONObject t = array.getJSONObject(i); String kind = t.optString("kind", "subtitles");
                    if (kind.equals("subtitles") || kind.equals("captions"))
                        for (MediaEntry e : pageItems) subtitle(e, r.url, t.optString("file"), t.optString("label"), t.optString("srclang"));
                }
            } catch (org.json.JSONException ignored) { }
        }
        if (depth < 2) for (Element frame : doc.select("iframe[src], iframe[data-src]")) {
            if (visited.size() >= 6) break;
            try {
                String value = frame.hasAttr("data-src") ? frame.attr("data-src") : frame.attr("src");
                String url = Urls.resolve(r.url, value);
                if (Urls.media(url)) add(url, title, r.url, "");
                else { Response child = fetch(url, r.url); if (child.media) add(child.url, title, r.url, child.type); else scan(child, depth + 1, title); }
            } catch (Exception error) {
                frameError = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
                note("Frame read failed type=" + error.getClass().getSimpleName());
            }
        }
    }
    private void subtitle(MediaEntry e, String base, String value, String label, String language) {
        if (e.subtitles.size() >= 50 || value.isEmpty()) return;
        try {
            String url = Urls.resolve(base, value);
            for (MediaEntry.Subtitle t : e.subtitles) if (t.url.equals(url)) return;
            String path = java.net.URI.create(url).getPath().toLowerCase(java.util.Locale.ROOT);
            String mime = path.endsWith(".srt") ? "application/x-subrip" : path.endsWith(".ass") || path.endsWith(".ssa") ? "text/x-ssa" : "text/vtt";
            if (language.isEmpty()) {
                if (label.equalsIgnoreCase("Turkish")) language = "tr";
                else if (label.equalsIgnoreCase("English") || label.equalsIgnoreCase("Eng Forced")) language = "en";
            }
            e.subtitles.add(new MediaEntry.Subtitle(url, label.isEmpty() ? "Altyazı" : M3u.shorten(label, 128), language, mime));
        } catch (IllegalArgumentException ignored) { }
    }
}
