package tr.hizlitv.player;

import tr.hizlitv.media.MediaEntry;
import tr.hizlitv.media.M3u;
import tr.hizlitv.media.Urls;

import org.junit.Test;
import static org.junit.Assert.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public class PhonePortalTest {
    @Test public void diagnosticsExplainRejectedRequestsWithoutLoggingSecrets() throws Exception {
        java.util.List<String> events = new java.util.concurrent.CopyOnWriteArrayList<>();
        String token = PhonePortal.newToken(), wrongToken = PhonePortal.newToken();
        try (PhonePortal portal = new PhonePortal(value -> true, token, 0, events::add)) {
            String address = portal.address("127.0.0.1");
            assertEquals(403, post(address, wrongToken, null, "https://private.example/movie"));
            assertEquals(400, request(address, token, null, "{\"url\":42}", "application/json"));
            String logged = String.join("\n", events);
            assertTrue(logged.contains("method=POST"));
            assertTrue(logged.contains("response code=403"));
            assertTrue(logged.contains("response code=400"));
            assertFalse(logged.contains(token)); assertFalse(logged.contains(wrongToken));
            assertFalse(logged.contains("private.example"));
            assertFalse(logged.contains("{\"url\""));
        }
    }
    @Test public void tokenAndOriginAreRequiredAndSubmittedTextIsNotExecuted() throws Exception {
        AtomicReference<String> text = new AtomicReference<>();
        try (PhonePortal portal = new PhonePortal(value -> { text.set(value); return true; })) {
            String address = portal.address("127.0.0.1"); String token = address.substring(address.lastIndexOf('/') + 1);
            HttpURLConnection get = (HttpURLConnection)new URL(address).openConnection();
            assertEquals(200, get.getResponseCode()); assertEquals("no-store", get.getHeaderField("Cache-Control")); get.disconnect();
            assertEquals(403, post(address, "", null, "https://example.org/video")); assertNull(text.get());
            assertEquals(403, post(address, token, "https://evil.example", "https://example.org/video")); assertNull(text.get());
            assertEquals(202, post(address, token, null, "https://example.org/video"));
            assertEquals("https://example.org/video", text.get());
            assertEquals(404, post(address + "x", token, null, "test"));
        }
    }
    private int post(String address, String token, String origin, String value) throws Exception {
        return request(address, token, origin, value, "text/plain;charset=UTF-8");
    }
    @Test public void shortcutJsonAndStablePairingSurviveListenerRestart() throws Exception {
        String token = PhonePortal.newToken(); AtomicReference<String> text = new AtomicReference<>();
        int port;
        try (PhonePortal first = new PhonePortal(value -> { text.set(value); return true; }, token, 0)) {
            port = first.port(); String address = first.address("127.0.0.1");
            assertEquals(202, request(address, token, null, "{\"url\":\"https://example.org/watch\"}", "application/json"));
            assertEquals("https://example.org/watch", text.get());
            assertEquals(400, request(address, token, null, "{\"url\":42}", "application/json"));
        }
        try (PhonePortal second = new PhonePortal(value -> true, token, port)) {
            assertEquals(202, post(second.address("127.0.0.1"), token, null, "https://example.org/new"));
            assertEquals(403, post(second.address("127.0.0.1"), PhonePortal.newToken(), null, "https://example.org/new"));
        }
    }
    private int request(String address, String token, String origin, String value, String type) throws Exception {
        // JDK HttpURLConnection silently drops the Origin header. Send the exact
        // hostile request over a raw local socket to test the server's actual check.
        URL url = new URL(address);
        try (java.net.Socket peer = new java.net.Socket(url.getHost(), url.getPort())) {
            peer.setSoTimeout(3000); byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            String headers = "POST " + url.getPath() + " HTTP/1.1\r\nHost: " + url.getAuthority() +
                "\r\nContent-Type: " + type + "\r\nX-HizliTV-Token: " + token +
                (origin == null ? "" : "\r\nOrigin: " + origin) + "\r\nContent-Length: " + bytes.length + "\r\n\r\n";
            peer.getOutputStream().write(headers.getBytes(StandardCharsets.US_ASCII)); peer.getOutputStream().write(bytes);
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(peer.getInputStream(), StandardCharsets.UTF_8));
            return Integer.parseInt(reader.readLine().split(" ")[1]);
        }
    }
}
