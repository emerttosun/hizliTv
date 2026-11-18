package tr.hizlitv.player.options;

import android.content.SharedPreferences;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import java.util.ArrayList;
import java.util.List;
import tr.hizlitv.ui.R;

/** Fit (default, keeps the aspect ratio), zoom (crop edges) or stretch; the choice is remembered. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
final class PictureOption implements PlayerOption {
    static final String KEY = "resize";
    static final int[] MODES = {AspectRatioFrameLayout.RESIZE_MODE_FIT, AspectRatioFrameLayout.RESIZE_MODE_ZOOM, AspectRatioFrameLayout.RESIZE_MODE_FILL};
    private static final int[] LABELS = {R.string.player_picture_fit, R.string.player_picture_zoom, R.string.player_picture_stretch};
    /** Saved index, clamped so an old or corrupt preference falls back to Fit. */
    static int saved(SharedPreferences preferences) { int i = preferences.getInt(KEY, 0); return i < 0 || i >= MODES.length ? 0 : i; }
    /** Applies the remembered mode to a new player view. */
    public static void restore(PlayerView view, SharedPreferences preferences) { view.setResizeMode(MODES[saved(preferences)]); }
    static int index(int mode) { for (int i = 0; i < MODES.length; i++) if (MODES[i] == mode) return i; return 0; }
    @Override public boolean available(Host host) { return true; }
    @Override public String title(Host host) { return host.context().getString(R.string.player_picture); }
    @Override public List<Choice> choices(Host host) {
        int current = index(host.view().getResizeMode()); List<Choice> out = new ArrayList<>();
        for (int i = 0; i < MODES.length; i++) {
            final int n = i;
            out.add(new Choice(host.context().getString(LABELS[i]), i == current, () -> {
                host.view().setResizeMode(MODES[n]); host.preferences().edit().putInt(KEY, n).apply();
            }));
        }
        return out;
    }
}
