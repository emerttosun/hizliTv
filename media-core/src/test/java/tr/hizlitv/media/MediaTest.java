package tr.hizlitv.media;

import tr.hizlitv.media.MediaEntry;
import tr.hizlitv.media.M3u;
import tr.hizlitv.media.Urls;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class MediaTest {
    @Test public void channelTitlesQuotesGroupsAndHeaders() {
        String data = "\ufeff#EXTM3U\r\n#EXTINF:-1 tvg-name=\"A, B\" group-title=\"Haber\",TRT Haber\r\n" +
            "#EXTVLCOPT:http-referrer=https://example.org/watch\n#EXTVLCOPT:http-user-agent=Custom/1\nhttps://example.org/live?id=a,b\n" +
            "#EXTINF:-1,İkinci\n#EXTGRP:Spor\nrelative.m3u8\n";
        List<MediaEntry> items = M3u.parse(data, "https://example.org/lists/main.m3u");
        assertEquals(2, items.size()); assertEquals("TRT Haber", items.get(0).title);
        assertEquals("Haber", items.get(0).group); assertEquals("https://example.org/watch", items.get(0).referer);
        assertEquals("Custom/1", items.get(0).userAgent); assertEquals("Spor", items.get(1).group);
        assertEquals("https://example.org/lists/relative.m3u8", items.get(1).url);
    }
    @Test public void hlsCannotBeMistakenForChannelList() {
        assertTrue(M3u.hls("#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1\nv.m3u8"));
        try { M3u.parse("#EXTM3U\n#EXT-X-TARGETDURATION:6\n#EXTINF:6,\nseg.ts", "https://example.org/"); fail(); }
        catch (IllegalArgumentException expected) { }
    }
    @Test public void urlSchemesHeaderInjectionAndInvalidEntriesAreRejected() {
        for (String value : new String[]{"file:///etc/passwd", "javascript:alert(1)", "https://x.test/\r\nCookie:secret", "not a URL"}) {
            try { Urls.http(value); fail(value); } catch (IllegalArgumentException expected) { }
        }
        try { M3u.safeHeader("hello\nInjected: y"); fail(); } catch (IllegalArgumentException expected) { }
        List<MediaEntry> items = M3u.parse("#EXTM3U\n#EXTINF:-1,Bad\nfile:///etc/passwd\n#EXTINF:-1,Good\nhttps://x.test/v\n", "");
        assertEquals(1, items.size()); assertEquals("Good", items.get(0).title);
    }
    @Test public void listSizeIsBounded() {
        StringBuilder s = new StringBuilder("#EXTM3U\n");
        for (int i = 0; i < 5100; i++) s.append("#EXTINF:-1,Channel ").append(i).append("\nhttps://x.test/").append(i).append('\n');
        assertEquals(5000, M3u.parse(s.toString(), "").size());
    }
}
