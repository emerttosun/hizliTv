package tr.hizlitv.player;

import tr.hizlitv.media.MediaEntry;
import tr.hizlitv.media.M3u;
import tr.hizlitv.media.Urls;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Short-lived local input page; no file endpoint, command endpoint, proxy, CORS or cloud. */
final class PhonePortal implements AutoCloseable {
    interface Receiver { boolean receive(String text); }
    interface Diagnostic { void accept(String event); }
    private final Receiver receiver;
    private final Diagnostic diagnostic;
    private final ServerSocket listener;
    private final String token;
    private final long expires;
    private volatile boolean running = true;
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(2, 2, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(4));
    PhonePortal(Receiver receiver) throws IOException {
        this(receiver, newToken(), 0, true, event -> { });
    }
    PhonePortal(Receiver receiver, String token, int port) throws IOException {
        this(receiver, token, port, false, event -> { });
    }
    PhonePortal(Receiver receiver, String token, int port, Diagnostic diagnostic) throws IOException {
        this(receiver, token, port, false, diagnostic);
    }
    private PhonePortal(Receiver receiver, String token, int port, boolean temporary, Diagnostic diagnostic) throws IOException {
        if (!token.matches("[a-f0-9]{48}")) throw new IllegalArgumentException("Invalid pairing identity");
        this.receiver = receiver; this.token = token; this.diagnostic = diagnostic;
        expires = temporary ? System.nanoTime() + TimeUnit.MINUTES.toNanos(10) : Long.MAX_VALUE;
        listener = new ServerSocket(); listener.setReuseAddress(true);
        listener.bind(new java.net.InetSocketAddress(port), 4); listener.setSoTimeout(1000);
        Thread thread = new Thread(this::accept, "PhonePortal"); thread.setDaemon(true); thread.start();
    }
    static String newToken() {
        byte[] random = new byte[24]; new SecureRandom().nextBytes(random);
        StringBuilder text = new StringBuilder(); for (byte b : random) text.append(String.format(Locale.US, "%02x", b & 255));
        return text.toString();
    }
    int port() { return listener.getLocalPort(); }
    String address(String ip) { return "http://" + ip + ":" + listener.getLocalPort() + "/" + token; }
    private void accept() {
        while (running && System.nanoTime() < expires) {
            try {
                Socket socket = listener.accept(); socket.setSoTimeout(3000);
                try { workers.execute(() -> handle(socket)); }
                catch (java.util.concurrent.RejectedExecutionException full) { socket.close(); }
            } catch (java.net.SocketTimeoutException ignored) { }
            catch (IOException e) { break; }
        }
        close();
    }
    private static String line(BufferedInputStream in, int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (out.size() >= limit) throw new IOException("header limit");
            if (c != '\r') out.write(c);
        }
        if (c < 0 && out.size() == 0) throw new IOException("EOF");
        return new String(out.toByteArray(), StandardCharsets.US_ASCII);
    }
    private void handle(Socket socket) {
        String phase = "request_line"; boolean turkish = false;
        note("connected");
        try (Socket peer = socket) {
            BufferedInputStream in = new BufferedInputStream(peer.getInputStream());
            String[] request = line(in, 8192).split(" ");
            if (request.length != 3) { reply(peer, 400, "text/plain", PhonePage.text("invalid_request", turkish)); return; }
            phase = "headers";
            Map<String,String> headers = new HashMap<>(); int size = 0;
            while (true) {
                String value = line(in, 4096); size += value.length(); if (size > 16384) throw new IOException("headers limit");
                if (value.isEmpty()) break; int colon = value.indexOf(':');
                if (colon <= 0) throw new IOException("header format");
                String name = value.substring(0, colon).toLowerCase(Locale.ROOT);
                if (headers.put(name, value.substring(colon + 1).trim()) != null) throw new IOException("duplicate header");
            }
            // Only fixed labels and booleans: never record paths, header values,
            // pairing identities, submitted URLs or exception messages.
            note("headers method=" + (request[0].equals("GET") ? "GET" : request[0].equals("POST") ? "POST" : "OTHER") +
                " length_present=" + headers.containsKey("content-length") +
                " transfer_encoding=" + headers.containsKey("transfer-encoding") +
                " expect_continue=" + "100-continue".equalsIgnoreCase(headers.get("expect")));
            turkish = PhonePage.turkish(header(headers, "accept-language", ""));
            phase = "validation";
            if (!running || System.nanoTime() >= expires) { reply(peer, 410, "text/plain", PhonePage.text("expired", turkish)); return; }
            String path = "/" + token;
            if (!request[1].equals(path)) { reply(peer, 404, "text/plain", PhonePage.text("not_found", turkish)); return; }
            if (request[0].equals("GET")) { reply(peer, 200, "text/html; charset=utf-8", PhonePage.render(token, turkish)); return; }
            if (!request[0].equals("POST")) { reply(peer, 405, "text/plain", PhonePage.text("not_supported", turkish)); return; }
            if (!MessageDigest.isEqual(token.getBytes(StandardCharsets.US_ASCII), header(headers, "x-hizlitv-token", "").getBytes(StandardCharsets.US_ASCII))) {
                reply(peer, 403, "text/plain", PhonePage.text("pairing", turkish)); return;
            }
            String origin = header(headers, "origin", "");
            if (!origin.isEmpty() && !origin.equals("http://" + header(headers, "host", ""))) { reply(peer, 403, "text/plain", PhonePage.text("origin", turkish)); return; }
            String contentType = header(headers, "content-type", "");
            if (headers.containsKey("transfer-encoding") || !(contentType.startsWith("text/plain") || contentType.startsWith("application/json"))) {
                reply(peer, 400, "text/plain", PhonePage.text("content_type", turkish)); return;
            }
            int length;
            try { length = Integer.parseInt(header(headers, "content-length", "-1")); } catch (NumberFormatException e) { length = -1; }
            if (length < 1 || length > 2_000_000) { reply(peer, 413, "text/plain", PhonePage.text("too_large", turkish)); return; }
            byte[] bytes = new byte[length]; int pos = 0, n;
            phase = "body";
            long bodyDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
            while (pos < length && (n = in.read(bytes, pos, length - pos)) > 0) {
                pos += n; if (System.nanoTime() > bodyDeadline) throw new IOException("body timeout");
            }
            if (pos != length) throw new IOException("short body");
            String submitted = new String(bytes, StandardCharsets.UTF_8);
            phase = "payload";
            if (contentType.startsWith("application/json")) {
                try {
                    Object value = new org.json.JSONObject(submitted).get("url");
                    if (!(value instanceof String)) throw new IllegalArgumentException();
                    submitted = (String)value;
                } catch (Exception malformed) { reply(peer, 400, "text/plain", PhonePage.text("json", turkish)); return; }
            }
            phase = "receiver";
            boolean accepted = receiver.receive(submitted);
            phase = "reply";
            reply(peer, accepted ? 202 : 409, "text/plain; charset=utf-8", accepted ? PhonePage.text("accepted", turkish) : PhonePage.text("busy", turkish));
        } catch (Exception error) {
            String reason = "unspecified";
            for (String known : new String[]{"EOF", "header limit", "headers limit", "header format", "duplicate header", "body timeout", "short body"})
                if (known.equals(error.getMessage())) reason = known.replace(' ', '_');
            note("failed phase=" + phase + " type=" + error.getClass().getSimpleName() + " reason=" + reason);
        }
    }
    private static String header(Map<String,String> headers, String key, String fallback) {
        String value = headers.get(key); return value == null ? fallback : value;
    }
    private void note(String event) { try { diagnostic.accept(event); } catch (Exception ignored) { } }
    private void reply(Socket peer, int code, String type, String body) throws IOException {
        note("response code=" + code);
        if (type.startsWith("text/") && !type.contains("charset=")) type += "; charset=utf-8";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 " + code + " Response\r\nContent-Type: " + type + "\r\nContent-Length: " + bytes.length +
            "\r\nConnection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\n" +
            "Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'\r\n\r\n";
        OutputStream out = peer.getOutputStream(); out.write(head.getBytes(StandardCharsets.US_ASCII)); out.write(bytes); out.flush();
    }
    public void close() { running = false; try { listener.close(); } catch (IOException ignored) { } workers.shutdown(); }
}
