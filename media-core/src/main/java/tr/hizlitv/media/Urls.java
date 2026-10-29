package tr.hizlitv.media;

import java.net.URI;
import java.util.Locale;

public final class Urls {
    public static String http(String raw) {
        if (raw == null || raw.length() > 8192 || raw.matches("(?s).*[\\r\\n\\x00].*"))
            throw new IllegalArgumentException("Geçerli bir HTTP/HTTPS bağlantısı gir.");
        URI u;
        try { u = URI.create(raw.trim()); } catch (Exception e) { throw new IllegalArgumentException("Bağlantı biçimi geçersiz."); }
        if (!("http".equalsIgnoreCase(u.getScheme()) || "https".equalsIgnoreCase(u.getScheme())) || u.getHost() == null)
            throw new IllegalArgumentException("Yalnızca HTTP/HTTPS bağlantıları destekleniyor.");
        return u.toASCIIString();
    }
    public static String resolve(String base, String value) {
        return http(URI.create(base).resolve(value.trim().replace("\\/", "/")).toString());
    }
    public static String host(String url) { try { return URI.create(url).getHost(); } catch (Exception e) { return ""; } }
    public static String mime(String url) {
        String p = URI.create(url).getPath().toLowerCase(Locale.ROOT);
        if (p.endsWith(".m3u8")) return "application/x-mpegURL";
        if (p.endsWith(".mpd")) return "application/dash+xml";
        if (p.endsWith(".mp4") || p.endsWith(".m4v")) return "video/mp4";
        if (p.endsWith(".mkv")) return "video/x-matroska";
        if (p.endsWith(".webm")) return "video/webm";
        if (p.endsWith(".ts")) return "video/mp2t";
        return "";
    }
    public static boolean media(String url) { return !mime(url).isEmpty(); }
}
