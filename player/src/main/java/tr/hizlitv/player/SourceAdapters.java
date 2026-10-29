package tr.hizlitv.player;

import java.util.Collections;
import java.util.List;

/** Explicit opt-in formats. The standard HTML resolver works with an empty registry. */
final class SourceAdapters {
    private SourceAdapters() { }
    static List<SourceAdapter> defaults() {
        return Collections.singletonList(new SourceAdapter() {
            public boolean supports(String host) { return host.equals("hdfilmcehennemi.mobi"); }
            public String extract(String html, String host) throws Exception { return ClosePageDecoder.decode(html, host); }
        });
    }
}
