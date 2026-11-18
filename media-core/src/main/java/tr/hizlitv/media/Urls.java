package tr.hizlitv.media;

import java.net.URI;
import java.util.Locale;

public final class Urls {
    public static String http(String raw) {
        if (raw == null || raw.length() > 8192 || raw.matches("(?s).*[\\r\\n\\x00].*"))
            throw new IllegalArgumentException("Enter a valid HTTP/HTTPS link.");
        URI u;
        try { u = URI.create(raw.trim()); } catch (Exception e) { throw new IllegalArgumentException("Invalid link format."); }
        if (!("http".equalsIgnoreCase(u.getScheme()) || "https".equalsIgnoreCase(u.getScheme())) || u.getHost() == null)
            throw new IllegalArgumentException("Only HTTP/HTTPS links are supported.");
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
    private static final java.util.Set<String> GENERIC = new java.util.HashSet<>(java.util.Arrays.asList(
        "index", "master", "playlist", "manifest", "video", "stream", "chunklist", "media", "live", "play", "output", "file", "download"));
    /** Readable title from a link's file name ("Big_Buck-Bunny.mp4" → "Big Buck Bunny"), or "" when it is generic or opaque. */
    public static String fileTitle(String url) {
        try {
            String path = URI.create(url).getRawPath(); if (path == null) return "";
            String name = java.net.URLDecoder.decode(path.substring(path.lastIndexOf('/') + 1).replace("+", "%2B"), "UTF-8");
            int dot = name.lastIndexOf('.'); if (dot > 0) name = name.substring(0, dot);
            name = name.replaceAll("[_.\\-]+", " ").replaceAll("(?<=\\p{Ll})(?=\\p{Lu})", " ").replaceAll("\\s+", " ").trim();
            if (name.length() < 3 || name.length() > 80 || GENERIC.contains(name.toLowerCase(Locale.ROOT))) return "";
            // Hashes, IDs and segment numbers are not titles.
            if (!name.matches(".*\\p{L}{3,}.*") || name.replace(" ", "").matches("(?i)[0-9a-f]{12,}")) return "";
            return name;
        } catch (Exception malformed) { return ""; }
    }
}
