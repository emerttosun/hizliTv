package tr.hizlitv.player.options;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;
import java.util.List;

/**
 * One entry of the player's options menu. To add an option, implement this interface and add it to
 * {@link PlayerOptions#defaults()}; the menu, focus, check marks and Back handling come from
 * {@link OptionsMenu}. Options never build dialogs themselves.
 */
public interface PlayerOption {
    /** What an option may read or change; implemented by the playback Activity. */
    interface Host {
        Context context();
        Player player();
        PlayerView view();
        SharedPreferences preferences();
        /** True for films that can be seeked (not live streams). */
        boolean seekable();
        /** Playback speed that survives channel changes, which rebuild the player. */
        float speed();
        void speed(float value);
    }
    /** A selectable value; {@code selected} draws the check mark. */
    final class Choice {
        public final String label;
        public final boolean selected;
        public final Runnable apply;
        public Choice(String label, boolean selected, Runnable apply) { this.label = label; this.selected = selected; this.apply = apply; }
    }
    /** Shown only when it applies to the current item. */
    boolean available(Host host);
    String title(Host host);
    /** Current value shown beside the title in the menu, or "" for none. */
    default String value(Host host) {
        for (Choice choice : choices(host)) if (choice.selected) return choice.label;
        return "";
    }
    List<Choice> choices(Host host);
}
