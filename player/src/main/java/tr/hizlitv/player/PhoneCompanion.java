package tr.hizlitv.player;

import tr.hizlitv.ui.R;
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
        catch (Exception failure) { events.status(activity.getString(R.string.text_the_phone_receiver_could_not_start_try_add_from_phone)); }
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
        if (fallback.isEmpty()) throw new java.io.IOException("No local network address was found."); return fallback;
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
            TextView instruction = TvUi.text(activity, activity.getString(R.string.text_scan_with_your_iphone_camera_paste_a_link_or_channel_list), 13, TvUi.MUTED, false);
            instruction.setGravity(Gravity.CENTER); instruction.setPadding(d(18), d(10), d(18), d(8)); content.addView(instruction);
            TextView manual = TvUi.text(activity, address, 10, TvUi.MUTED, false); manual.setGravity(Gravity.CENTER); manual.setPadding(d(12), 0, d(12), d(10)); content.addView(manual);
            dialog = new AlertDialog.Builder(activity).setTitle(activity.getString(R.string.text_add_from_phone)).setView(content)
                .setNeutralButton(activity.getString(R.string.text_forget_pairing), (d, w) -> confirmForget()).setNegativeButton(activity.getString(R.string.text_close), null).create();
            dialog.setOnDismissListener(d -> dialog = null); dialog.show(); events.audit("phone_pairing_open");
        } catch (Exception failure) { stop(); events.status(activity.getString(R.string.text_phone_transfer_could_not_start_check_that_both_devices_are)); }
    }
    private int d(int value) { return TvUi.dp(activity, value); }
    private void confirmForget() {
        new AlertDialog.Builder(activity).setTitle(activity.getString(R.string.text_forget_phone_pairing))
            .setMessage(activity.getString(R.string.text_the_current_qr_address_and_iphone_shortcut_will_stop_worki))
            .setPositiveButton(activity.getString(R.string.text_forget), (d, w) -> { stop(); preferences().edit().clear().commit(); events.audit("phone_pairing_forgotten"); events.status(activity.getString(R.string.text_phone_pairing_forgotten)); })
            .setNegativeButton(activity.getString(R.string.text_cancel), null).show();
    }
    void closeDialog() { if (dialog != null) { dialog.dismiss(); dialog = null; } }
    void stop() {
        closeDialog(); if (portal != null) { portal.close(); portal = null; }
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
