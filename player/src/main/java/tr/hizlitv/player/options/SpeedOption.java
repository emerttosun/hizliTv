package tr.hizlitv.player.options;

import androidx.media3.common.PlaybackParameters;
import java.util.ArrayList;
import java.util.List;
import tr.hizlitv.ui.R;

/** Playback speed for films; hidden for live streams, where it would only drift from the live edge. */
final class SpeedOption implements PlayerOption {
    static final float[] SPEEDS = {0.75f, 1f, 1.25f, 1.5f, 2f};
    /** "0.75x", "1.5x", "2x"; null for normal speed, which uses the translated word. */
    static String multiplier(float value) {
        if (value == 1f) return null;
        return (value == (int) value ? String.valueOf((int) value) : String.valueOf(value)) + "x";
    }
    private static String label(Host host, float value) { String x = multiplier(value); return x == null ? host.context().getString(R.string.player_speed_normal) : x; }
    @Override public boolean available(Host host) { return host.seekable(); }
    @Override public String title(Host host) { return host.context().getString(R.string.player_speed); }
    @Override public List<Choice> choices(Host host) {
        List<Choice> out = new ArrayList<>();
        for (float value : SPEEDS) out.add(new Choice(label(host, value), value == host.speed(), () -> {
            host.speed(value); host.player().setPlaybackParameters(new PlaybackParameters(value));
        }));
        return out;
    }
}
