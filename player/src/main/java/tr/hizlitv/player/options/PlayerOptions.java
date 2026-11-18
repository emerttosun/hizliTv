package tr.hizlitv.player.options;

import androidx.media3.common.C;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The options menu, in display order. Reorder, remove or add entries here only. */
public final class PlayerOptions {
    private PlayerOptions() { }
    public static List<PlayerOption> defaults() {
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
            new TrackOption(C.TRACK_TYPE_VIDEO),
            new TrackOption(C.TRACK_TYPE_AUDIO),
            new TrackOption(C.TRACK_TYPE_TEXT),
            new PictureOption(),
            new SpeedOption())));
    }
    /** Applies the remembered picture mode to a new player view. */
    public static void restorePicture(androidx.media3.ui.PlayerView view, android.content.SharedPreferences preferences) { PictureOption.restore(view, preferences); }
}
