package tr.hizlitv.media;

import java.util.ArrayList;
import java.util.List;

/** URLs may contain provider tokens: never include them in logs. */
public final class MediaEntry {
    public String title = "Video", url = "", group = "", referer = "", userAgent = "", mime = "", sourcePage = "";
    public boolean favorite;
    public final List<Subtitle> subtitles = new ArrayList<>();
    public static final class Subtitle {
        public String url, label, language, mime;
        public Subtitle(String url, String label, String language, String mime) {
            this.url = url; this.label = label; this.language = language; this.mime = mime;
        }
    }
}
