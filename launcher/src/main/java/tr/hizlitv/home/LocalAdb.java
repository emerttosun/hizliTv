package tr.hizlitv.home;

import com.cgutman.adblib.AdbBase64;
import com.cgutman.adblib.AdbCrypto;
import com.cgutman.adblib.AdbProtocol;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Authenticated connection to this device only. No command intent, network listener or service. */
public final class LocalAdb {
    private final File directory;
    private final AdbBase64 base64;
    private final int port;
    private static final Set<String> PROTECTED = new HashSet<>(Arrays.asList(
        "android", "tr.hizlitv.home", "com.google.android.tvlauncher", "com.android.systemui",
        "com.android.tv.settings", "io.github.jqssun.airplay", "com.boost.airplay.receiver"));

    public LocalAdb(File directory, AdbBase64 base64) { this(directory, base64, 5555); }
    // Alternate port exists only for isolated local protocol tests.
    LocalAdb(File directory, AdbBase64 base64, int port) {
        this.directory = directory; this.base64 = base64; this.port = port;
    }
    private synchronized AdbCrypto identity() throws Exception {
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("ADB anahtar klasörü oluşturulamadı.");
        File secret = new File(directory, "adb-private.pk8");
        File pub = new File(directory, "adb-public.x509");
        if (secret.exists() && pub.exists()) return AdbCrypto.loadAdbKeyPair(base64, secret, pub);
        AdbCrypto crypto = AdbCrypto.generateAdbKeyPair(base64);
        crypto.saveAdbKeyPair(secret, pub);
        secret.setReadable(false, false); secret.setWritable(false, false);
        secret.setReadable(true, true); secret.setWritable(true, true);
        return crypto;
    }
    private static AdbProtocol.AdbMessage read(InputStream in) throws IOException {
        AdbProtocol.AdbMessage packet = AdbProtocol.AdbMessage.parseAdbMessage(in);
        if (!AdbProtocol.validateMessage(packet)) throw new IOException("ADB paketi doğrulanamadı.");
        return packet;
    }
    private static void write(OutputStream out, byte[] bytes) throws IOException { out.write(bytes); out.flush(); }
    private String execute(String command, boolean pairing, Runnable request) throws Exception {
        AdbCrypto crypto = identity();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 3000);
            socket.setTcpNoDelay(true); socket.setSoTimeout(pairing ? 60000 : 10000);
            InputStream in = socket.getInputStream(); OutputStream out = socket.getOutputStream();
            write(out, AdbProtocol.generateConnect());
            boolean sentSignature = false, connected = false;
            for (int attempts = 0; attempts < 8; attempts++) {
                AdbProtocol.AdbMessage packet = read(in);
                if (packet.command == AdbProtocol.CMD_CNXN) { connected = true; break; }
                if (packet.command != AdbProtocol.CMD_AUTH || packet.arg0 != AdbProtocol.AUTH_TYPE_TOKEN)
                    throw new IOException("ADB eşleştirmesi tamamlanamadı.");
                if (!sentSignature) {
                    write(out, AdbProtocol.generateAuth(AdbProtocol.AUTH_TYPE_SIGNATURE, crypto.signAdbTokenPayload(packet.payload)));
                    sentSignature = true;
                } else {
                    if (!pairing) throw new IOException("Önce ADB bağla ile TV’de HızlıTV’ye izin ver.");
                    if (request != null) request.run();
                    write(out, AdbProtocol.generateAuth(AdbProtocol.AUTH_TYPE_RSA_PUBLIC, crypto.getAdbPublicKeyPayload()));
                }
            }
            if (!connected) throw new IOException("ADB bağlantısı açılamadı.");
            socket.setSoTimeout(10000);
            write(out, AdbProtocol.generateOpen(1, "shell:" + command));
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            int remote = 0; boolean opened = false;
            while (true) {
                AdbProtocol.AdbMessage packet = read(in);
                if (packet.arg1 != 1) throw new IOException("Beklenmeyen ADB akışı.");
                if (packet.command == AdbProtocol.CMD_OKAY) { remote = packet.arg0; opened = true; }
                else if (packet.command == AdbProtocol.CMD_WRTE && opened) {
                    if (result.size() + packet.payloadLength > 2000000) throw new IOException("ADB çıktısı çok büyük.");
                    if (packet.payload != null) result.write(packet.payload);
                    write(out, AdbProtocol.generateReady(1, remote));
                } else if (packet.command == AdbProtocol.CMD_CLSE) {
                    if (!opened) throw new IOException("ADB komutu kabul edilmedi.");
                    write(out, AdbProtocol.generateClose(1, remote));
                    return new String(result.toByteArray(), StandardCharsets.UTF_8);
                } else throw new IOException("ADB akışı tamamlanamadı.");
            }
        }
    }
    public void authorize(Runnable request) throws Exception {
        String id = execute("id", true, request);
        if (!id.contains("uid=2000(shell)")) throw new IOException("Beklenen rootsuz Android kabuğu doğrulanamadı.");
    }
    public String memoryDump() throws Exception { return execute("dumpsys meminfo", false, null); }
    public static void validateTarget(String pkg) {
        if (pkg == null || !pkg.matches("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+") || PROTECTED.contains(pkg))
            throw new IllegalArgumentException("Bu paket kapatma için uygun değil.");
    }
    public void stopPackage(String pkg) throws Exception {
        validateTarget(pkg);
        // A marker gives shell-v1 an explicit exit status; the target accepts no shell metacharacters.
        String response = execute("am force-stop --user 0 " + pkg + "; echo __HIZLITV_RC=$?", false, null);
        if (!response.trim().endsWith("__HIZLITV_RC=0")) throw new IOException("Android uygulamayı durduramadı: " + pkg);
    }
}
