package tr.hizlitv.media;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class M3u {
    private static final Pattern ATTR = Pattern.compile("([\\w-]+)=\"([^\"]*)\"");
    public static boolean hls(String value) { return value.contains("#EXT-X-"); }
    public static List<MediaEntry> parse(String value, String base) {
        if (value.length() > 2_000_000) throw new IllegalArgumentException("Liste 2 MB sınırını aşıyor.");
        if (hls(value)) throw new IllegalArgumentException("Bu bir HLS video yayını; kanal listesi değil.");
        ArrayList<MediaEntry> out = new ArrayList<>();
        MediaEntry next = new MediaEntry();
        for (String raw : value.replace("\ufeff", "").split("\\r?\\n")) {
            String line = raw.trim();
            if (line.startsWith("#EXTINF:")) {
                next = new MediaEntry();
                // A comma inside a quoted attribute is not the title separator.
                boolean quote = false; int split = -1;
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i); if (c == '"') quote = !quote;
                    if (c == ',' && !quote) { split = i; break; }
                }
                if (split >= 0 && split + 1 < line.length()) next.title = shorten(line.substring(split + 1).trim(), 256);
                Matcher a = ATTR.matcher(split < 0 ? line : line.substring(0, split));
                while (a.find()) if (a.group(1).equals("group-title")) next.group = shorten(a.group(2), 128);
            } else if (line.startsWith("#EXTGRP:")) next.group = shorten(line.substring(8).trim(), 128);
            else if (line.startsWith("#EXTVLCOPT:http-referrer=")) next.referer = Urls.http(line.substring(25));
            else if (line.startsWith("#EXTVLCOPT:http-user-agent=")) next.userAgent = safeHeader(line.substring(27));
            else if (!line.isEmpty() && !line.startsWith("#")) {
                try {
                    next.url = base.isEmpty() ? Urls.http(line) : Urls.resolve(base, line);
                    next.mime = Urls.mime(next.url);
                    out.add(next); next = new MediaEntry();
                    if (out.size() >= 5000) break;
                } catch (IllegalArgumentException ignored) { next = new MediaEntry(); }
            }
        }
        if (out.isEmpty()) throw new IllegalArgumentException("Listede geçerli kanal bağlantısı bulunamadı.");
        return out;
    }
    public static String safeHeader(String value) {
        if (value.length() > 1024 || value.matches("(?s).*[\\r\\n\\x00].*")) throw new IllegalArgumentException("Geçersiz yayın başlığı.");
        return value;
    }
    public static String shorten(String value, int limit) { return value.length() > limit ? value.substring(0, limit) : value; }
}
