package tr.hizlitv.player;

import tr.hizlitv.media.MediaEntry;
import tr.hizlitv.media.M3u;
import tr.hizlitv.media.Urls;

import org.junit.Test;
import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ResolverTest {
    @Test public void injectedAdapterDoesNotRequireEditingTheStandardHtmlResolver() throws Exception {
        Fixture server = new Fixture();
        String base = "http://127.0.0.1:" + server.port();
        response(server, "/custom", "text/html", "<title>Custom source</title><p>owned fixture</p>");
        server.start();
        try {
            SourceAdapter adapter = new SourceAdapter() {
                public boolean supports(String host) { return host.equals("127.0.0.1"); }
                public String extract(String html, String host) { return "https://media.example.org/custom.m3u8"; }
            };
            Resolver.Result result = new Resolver(java.util.Collections.singletonList(adapter)).resolve(base + "/custom");
            assertEquals("https://media.example.org/custom.m3u8", result.items.get(0).url);
            try { new Resolver(java.util.Collections.emptyList()).resolve(base + "/custom"); fail(); }
            catch (java.io.IOException expected) { assertTrue(expected.getMessage().contains("bulunamadı")); }
        } finally { server.close(); }
    }
    @Test public void findsIframeVideoAndSubtitlesWithoutLoadingAdScripts() throws Exception {
        Fixture server = new Fixture();
        String base = "http://127.0.0.1:" + server.port();
        response(server, "/page", "text/html", "<title>Test Film</title><iframe data-src='/embed'></iframe><script src='/ads.js'></script>");
        response(server, "/embed", "text/html", "<video><source src='/film.mp4' type='video/mp4'><track src='/tr.vtt' srclang='tr' label='Türkçe'></video>");
        java.util.concurrent.atomic.AtomicBoolean adRequested = new java.util.concurrent.atomic.AtomicBoolean();
        server.adRequested = adRequested;
        server.start();
        try {
            Resolver.Result r = new Resolver().resolve(base + "/page");
            assertFalse(r.playlist); assertEquals(1, r.items.size()); MediaEntry e = r.items.get(0);
            assertEquals("Test Film", e.title); assertEquals(base + "/film.mp4", e.url);
            assertEquals(base + "/embed", e.referer); assertEquals(base + "/page", e.sourcePage);
            assertEquals(1, e.subtitles.size()); assertEquals("tr", e.subtitles.get(0).language);
            assertFalse(adRequested.get());
        } finally { server.close(); }
    }
    @Test public void sniffPlaylistEvenWithM3u8ExtensionAndTreatTxtHlsAsVideo() throws Exception {
        Fixture server = new Fixture();
        String base = "http://127.0.0.1:" + server.port();
        response(server, "/channels.m3u8", "application/vnd.apple.mpegurl", "#EXTM3U\n#EXTINF:-1,Test Kanal\n/live.m3u8\n");
        response(server, "/master.txt", "application/vnd.apple.mpegurl", "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1200000\nvideo.txt\n");
        server.start();
        try {
            Resolver.Result channels = new Resolver().resolve(base + "/channels.m3u8");
            assertTrue(channels.playlist); assertEquals(base + "/live.m3u8", channels.items.get(0).url);
            Resolver.Result hls = new Resolver().resolve(base + "/master.txt");
            assertFalse(hls.playlist); assertEquals("application/x-mpegURL", hls.items.get(0).mime);
        } finally { server.close(); }
    }
    @Test public void returnsRealHttpFailureAndDoesNotInventSources() throws Exception {
        Fixture server = new Fixture();
        server.denied = true; server.start();
        try {
            try { new Resolver().resolve("http://127.0.0.1:" + server.port() + "/denied"); fail(); }
            catch (java.io.IOException expected) { assertTrue(expected.getMessage().contains("403")); }
        } finally { server.close(); }
    }
    private void response(Fixture server, String path, String type, String value) {
        server.responses.put(path, new String[]{type, value});
    }
    /** Minimal isolated HTTP peer for resolver integration tests; no production dependency. */
    private static final class Fixture implements AutoCloseable {
        final java.net.ServerSocket listener;
        final java.util.Map<String,String[]> responses = new java.util.HashMap<>();
        java.util.concurrent.atomic.AtomicBoolean adRequested;
        volatile boolean denied, running = true;
        Thread thread;
        Fixture() throws Exception { listener = new java.net.ServerSocket(0, 4, java.net.InetAddress.getByName("127.0.0.1")); }
        int port() { return listener.getLocalPort(); }
        void start() {
            thread = new Thread(() -> {
                while (running) try (java.net.Socket peer = listener.accept()) {
                    peer.setSoTimeout(3000);
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(peer.getInputStream(), StandardCharsets.US_ASCII));
                    String request = reader.readLine(); if (request == null) continue;
                    String path = request.split(" ")[1]; String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) { }
                    if (path.equals("/ads.js") && adRequested != null) adRequested.set(true);
                    String[] response = responses.get(path); int code = denied ? 403 : response == null ? 404 : 200;
                    byte[] body = response == null ? new byte[0] : response[1].getBytes(StandardCharsets.UTF_8);
                    String head = "HTTP/1.1 " + code + " Response\r\nContent-Type: " + (response == null ? "text/plain" : response[0]) +
                        "\r\nContent-Length: " + body.length + "\r\nConnection: close\r\n\r\n";
                    peer.getOutputStream().write(head.getBytes(StandardCharsets.US_ASCII)); peer.getOutputStream().write(body);
                } catch (Exception e) { if (running) throw new RuntimeException(e); }
            });
            thread.setDaemon(true); thread.start();
        }
        public void close() throws Exception { running = false; listener.close(); if (thread != null) thread.join(1000); }
    }
}
