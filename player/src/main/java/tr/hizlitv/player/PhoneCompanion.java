package tr.hizlitv.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Collections;
import tr.hizlitv.ui.TvUi;

/** Owns pairing identity, local HTTP lifetime and QR presentation, independent of library UI. */
final class PhoneCompanion {
    interface Events { void status(String text); void audit(String event); }
    private final Activity activity;
    private final PhonePortal.Receiver receiver;
    private final Events events;
    private PhonePortal portal;
    private AlertDialog dialog;
    PhoneCompanion(Activity activity, PhonePortal.Receiver receiver, Events events) { this.activity = activity; this.receiver = receiver; this.events = events; }
    private SharedPreferences preferences() { return activity.getSharedPreferences("phone", 0); }
    void onStart() {
        if (!preferences().getString("token", "").isEmpty()) try { start(); }
        catch (Exception failure) { events.status("Telefon alıcısı açılamadı; Telefondan ekle ile tekrar dene."); }
    }
    private void start() throws Exception {
        if (portal != null) return;
        SharedPreferences prefs = preferences(); String token = prefs.getString("token", "");
        if (token.isEmpty()) { token = PhonePortal.newToken(); if (!prefs.edit().putString("token", token).commit()) throw new java.io.IOException("Identity not saved"); }
        portal = new PhonePortal(receiver, token, prefs.getInt("port", 8198), event -> android.util.Log.i("HizliTV.Phone", event));
        prefs.edit().putInt("port", portal.port()).apply();
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    static String localIp() throws Exception {
        String fallback = "";
        for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!network.isUp() || network.isLoopback()) continue;
            for (java.net.InetAddress address : Collections.list(network.getInetAddresses())) if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                if (network.getName().startsWith("wlan") || network.getName().startsWith("eth")) return address.getHostAddress();
                fallback = address.getHostAddress();
            }
        }
        if (fallback.isEmpty()) throw new java.io.IOException("Yerel ağ adresi bulunamadı."); return fallback;
    }
    void open() {
        closeDialog();
        try {
            start(); String address = portal.address(localIp());
            LinearLayout content = new LinearLayout(activity); content.setOrientation(LinearLayout.VERTICAL); content.setGravity(Gravity.CENTER);
            ImageView qr = new ImageView(activity); int size = 440;
            BitMatrix matrix = new MultiFormatWriter().encode(address, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565); int[] pixels = new int[size * size];
            for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) pixels[y * size + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size); qr.setImageBitmap(bitmap);
            content.addView(qr, new LinearLayout.LayoutParams(d(180), d(180)));
            TextView instruction = TvUi.text(activity, "iPhone kameranla okut. Bağlantını veya kanal listeni yapıştır.\nSonraki gönderimlerde Paylaş → TV’de izle kestirmeni kullanabilirsin.\nGönderirken TV’de bu kütüphane ekranı açık olsun.", 13, TvUi.MUTED, false);
            instruction.setGravity(Gravity.CENTER); instruction.setPadding(d(18), d(10), d(18), d(8)); content.addView(instruction);
            TextView manual = TvUi.text(activity, address, 10, TvUi.MUTED, false); manual.setGravity(Gravity.CENTER); manual.setPadding(d(12), 0, d(12), d(10)); content.addView(manual);
            dialog = new AlertDialog.Builder(activity).setTitle("Telefondan ekle").setView(content)
                .setNeutralButton("Eşleştirmeyi unut", (d, w) -> confirmForget()).setNegativeButton("Kapat", null).create();
            dialog.setOnDismissListener(d -> dialog = null); dialog.show(); events.audit("phone_pairing_open");
        } catch (Exception failure) { stop(); events.status("Telefon aktarımı açılamadı. Aynı yerel ağa bağlı olduğunu kontrol et."); }
    }
    private int d(int value) { return TvUi.dp(activity, value); }
    private void confirmForget() {
        new AlertDialog.Builder(activity).setTitle("Telefon eşleştirmesi unutulsun mu?")
            .setMessage("Mevcut QR adresi ve iPhone kestirmesi çalışmayacak. Yeniden eşleştirebilirsin.")
            .setPositiveButton("Unut", (d, w) -> { stop(); preferences().edit().clear().commit(); events.audit("phone_pairing_forgotten"); events.status("Telefon eşleştirmesi unutuldu."); })
            .setNegativeButton("Vazgeç", null).show();
    }
    void closeDialog() { if (dialog != null) { dialog.dismiss(); dialog = null; } }
    void stop() {
        closeDialog(); if (portal != null) { portal.close(); portal = null; }
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
