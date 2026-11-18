package tr.hizlitv.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.view.Display;
import java.util.Locale;

/**
 * What differs between TV models, read from the device at runtime instead of being hard-coded
 * for one model. Porting to another Android TV should not require editing these values; the
 * pure static rules below are unit tested.
 */
public final class DeviceProfile {
    /** Largest display mode the panel supports, in physical pixels (a 4K TV may run its UI at 1080p). */
    public final int panelWidth, panelHeight;
    public final long totalRamMb;
    public final boolean lowRam;
    public final int sdk;
    public final String model;
    private DeviceProfile(int panelWidth, int panelHeight, long totalRamMb, boolean lowRam, int sdk, String model) {
        this.panelWidth = panelWidth; this.panelHeight = panelHeight; this.totalRamMb = totalRamMb; this.lowRam = lowRam; this.sdk = sdk; this.model = model;
    }
    public static DeviceProfile of(Context context) {
        int width = 1920, height = 1080;
        try {
            Display display = ((DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE)).getDisplay(Display.DEFAULT_DISPLAY);
            if (display != null) for (Display.Mode mode : display.getSupportedModes()) {
                int w = Math.max(mode.getPhysicalWidth(), mode.getPhysicalHeight()), h = Math.min(mode.getPhysicalWidth(), mode.getPhysicalHeight());
                if ((long) w * h > (long) width * height) { width = w; height = h; }
            }
        } catch (Exception ignored) { }
        long ram = 0; boolean low = true;
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo(); manager.getMemoryInfo(info);
            ram = info.totalMem / 1048576; low = manager.isLowRamDevice();
        } catch (Exception ignored) { }
        return new DeviceProfile(width, height, ram, low, Build.VERSION.SDK_INT, Build.MANUFACTURER + " " + Build.MODEL);
    }
    /** Video cap: 2160p only on a 4K panel with enough memory, otherwise 1080p (the old fixed value). */
    public int[] maxVideoSize() { return maxVideoSize(panelWidth, panelHeight, lowRam, totalRamMb); }
    public static int[] maxVideoSize(int panelWidth, int panelHeight, boolean lowRam, long ramMb) {
        boolean uhd = panelWidth >= 3840 && panelHeight >= 2160 && !lowRam && ramMb >= 1800;
        return uhd ? new int[]{3840, 2160} : new int[]{1920, 1080};
    }
    /** Playback buffer: 24 MB (the tested value) up to 1080p; only devices that play 4K hold more. */
    public int bufferBytes() { int[] video = maxVideoSize(); return bufferBytes(video[1] >= 2160, totalRamMb); }
    public static int bufferBytes(boolean uhd, long ramMb) {
        if (!uhd) return 24 * 1024 * 1024;
        return ramMb >= 3000 ? 96 * 1024 * 1024 : 48 * 1024 * 1024;
    }
    /** One line for logcat when checking a new TV; contains no user data. */
    public String summary() {
        return String.format(Locale.US, "model=%s sdk=%d panel=%dx%d ram=%dMB lowRam=%b video=%dx%d bufferMB=%d",
            model, sdk, panelWidth, panelHeight, totalRamMb, lowRam, maxVideoSize()[0], maxVideoSize()[1], bufferBytes() / 1048576);
    }
}
