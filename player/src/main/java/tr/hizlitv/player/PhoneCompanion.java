package tr.hizlitv.player;

import tr.hizlitv.ui.R;
import android.app.Activity;
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
    interface Events {
        void status(String text); void audit(String event);
        /** Inline pairing panel; {@code qr} is null when the receiver or network is unavailable. */
        void pairing(Bitmap qr, String address);
    }
    private final Activity activity;
    private final PhonePortal.Receiver receiver;
    private final Events events;
    private PhonePortal portal;
    private tr.hizlitv.ui.TvDialog dialog;
    PhoneCompanion(Activity activity, PhonePortal.Receiver receiver, Events events) { this.activity = activity; this.receiver = receiver; this.events = events; }
    private SharedPreferences preferences() { return activity.getSharedPreferences("phone", 0); }
    /** Pairing is created on first use so the QR code is ready without an extra dialog. */
    void onStart() {
        try { start(); }
        catch (Exception failure) { events.pairing(null, ""); events.status(activity.getString(R.string.phone_receiver_could_not_start)); return; }
        publish();
    }
    private void publish() {
        try { String address = portal.address(localIp()); events.pairing(qr(address, 360), address); }
        catch (Exception offline) { events.pairing(null, ""); }
    }
    private static Bitmap qr(String address, int size) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(address, BarcodeFormat.QR_CODE, size, size);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565); int[] pixels = new int[size * size];
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) pixels[y * size + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size); return bitmap;
    }
    private void start() throws Exception {
        if (portal != null) return;
        SharedPreferences prefs = preferences(); String token = prefs.getString("token", "");
        if (token.isEmpty()) { token = PhonePortal.newToken(); if (!prefs.edit().putString("token", token).commit()) throw new java.io.IOException("Identity not saved"); }
        PhonePortal.Diagnostic log = event -> android.util.Log.i("HizliTV.Phone", event);
        // Keep the saved port so QR codes and shortcuts stay valid; if another app on this TV
        // already uses it, take any free port instead of failing (the QR code then shows the new one).
        try { portal = new PhonePortal(receiver, token, prefs.getInt("port", 8198), log); }
        catch (java.net.BindException taken) { portal = new PhonePortal(receiver, token, 0, log); events.audit("phone_port_changed"); }
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
        if (fallback.isEmpty()) throw new java.io.IOException("No local network address was found."); return fallback;
    }
    void open() {
        closeDialog();
        try {
            start(); String address = portal.address(localIp());
            LinearLayout content = new LinearLayout(activity); content.setOrientation(LinearLayout.VERTICAL); content.setGravity(Gravity.CENTER);
            ImageView qr = new ImageView(activity); qr.setImageBitmap(qr(address, 440));
            content.addView(qr, new LinearLayout.LayoutParams(d(180), d(180)));
            qr.setBackgroundColor(Color.WHITE);
            TextView manual = TvUi.text(activity, address, 11, TvUi.MUTED, false); manual.setGravity(Gravity.CENTER); manual.setPadding(d(12), d(10), d(12), 0); content.addView(manual);
            tr.hizlitv.ui.TvDialog shown = new tr.hizlitv.ui.TvDialog(activity, activity.getString(R.string.player_eyebrow), activity.getString(R.string.text_add_from_phone),
                activity.getString(R.string.text_scan_with_your_iphone_camera_paste_a_link_or_channel_list))
                .content(content).action(activity.getString(R.string.text_close), null).action(activity.getString(R.string.phone_new_qr_code), this::confirmForget);
            dialog = shown; shown.setOnDismissListener(d -> { if (dialog == shown) dialog = null; }); shown.show(); events.audit("phone_pairing_open");
        } catch (Exception failure) {
            // Keep a working receiver; only the enlarged QR view failed (usually no local address).
            closeDialog(); publish(); events.status(activity.getString(R.string.text_phone_transfer_could_not_start_check_that_both_devices_are));
        }
    }
    private int d(int value) { return TvUi.dp(activity, value); }
    private void confirmForget() {
        // Cancel is first, so a stray OK never invalidates the phone's shortcut.
        new tr.hizlitv.ui.TvDialog(activity, activity.getString(R.string.player_eyebrow), activity.getString(R.string.text_forget_phone_pairing),
            activity.getString(R.string.text_the_current_qr_address_and_iphone_shortcut_will_stop_worki))
            .action(activity.getString(R.string.text_cancel), null)
            .action(activity.getString(R.string.text_forget), () -> {
                stop(); preferences().edit().clear().commit(); events.audit("phone_pairing_forgotten");
                // A fresh identity replaces the old one immediately; the panel shows the new QR code.
                onStart(); if (portal != null) events.status(activity.getString(R.string.phone_pairing_renewed));
            }).show();
    }
    void closeDialog() { if (dialog != null) { dialog.dismiss(); dialog = null; } }
    void stop() {
        closeDialog(); if (portal != null) { portal.close(); portal = null; }
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
