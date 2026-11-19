package tr.hizlitv.player.options;

import android.content.SharedPreferences;
import androidx.media3.common.C;
import androidx.media3.common.Tracks;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;
import java.util.ArrayList;
import java.util.List;
import tr.hizlitv.ui.R;

/** Subtitle text size as a share of the video height; remembered, and shown only when the source has subtitles. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
final class SubtitleSizeOption implements PlayerOption {
    static final String KEY = "subtitle_size";
    /** Small, normal (Media3's default), large, extra large. */
    static final float[] FRACTIONS = {0.045f, SubtitleView.DEFAULT_TEXT_SIZE_FRACTION, 0.065f, 0.08f};
    static final int DEFAULT = 1;
    private static final int[] LABELS = {R.string.player_size_small, R.string.player_size_normal, R.string.player_size_large, R.string.player_size_xlarge};
    static int saved(SharedPreferences preferences) { int i = preferences.getInt(KEY, DEFAULT); return i < 0 || i >= FRACTIONS.length ? DEFAULT : i; }
    static void restore(PlayerView view, SharedPreferences preferences) {
        SubtitleView subtitles = view.getSubtitleView(); if (subtitles != null) subtitles.setFractionalTextSize(FRACTIONS[saved(preferences)]);
    }
    @Override public boolean available(Host host) {
        for (Tracks.Group g : host.player().getCurrentTracks().getGroups()) if (g.getType() == C.TRACK_TYPE_TEXT && g.isSupported()) return true;
        return false;
    }
    @Override public String title(Host host) { return host.context().getString(R.string.player_subtitle_size); }
    @Override public List<Choice> choices(Host host) {
        int current = saved(host.preferences()); List<Choice> out = new ArrayList<>();
        for (int i = 0; i < FRACTIONS.length; i++) {
            final int n = i;
            out.add(new Choice(host.context().getString(LABELS[i]), i == current, () -> {
                host.preferences().edit().putInt(KEY, n).apply(); restore(host.view(), host.preferences());
            }));
        }
        return out;
    }
}
