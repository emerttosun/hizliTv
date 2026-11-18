package tr.hizlitv.player.options;

import android.app.Activity;
import java.util.ArrayList;
import java.util.List;
import tr.hizlitv.ui.R;
import tr.hizlitv.ui.TvDialog;

/** Renders any list of {@link PlayerOption}s as TV panels: the menu, one sub-list each, Back to the menu. */
public final class OptionsMenu {
    /** Shows a dialog and owns its lifecycle (dismissed when playback stops). */
    public interface Prompter { void show(TvDialog dialog); }
    private final Activity activity;
    private final PlayerOption.Host host;
    private final List<PlayerOption> options;
    private final Prompter prompter;
    public OptionsMenu(Activity activity, PlayerOption.Host host, List<PlayerOption> options, Prompter prompter) {
        this.activity = activity; this.host = host; this.options = options; this.prompter = prompter;
    }
    public void open() {
        if (host.player() == null) return;
        List<PlayerOption> shown = new ArrayList<>(); List<String> labels = new ArrayList<>();
        for (PlayerOption option : options) {
            if (!option.available(host)) continue;
            String value = option.value(host); shown.add(option);
            labels.add(value.isEmpty() ? option.title(host) : option.title(host) + "  ·  " + value);
        }
        prompter.show(new TvDialog(activity, activity.getString(R.string.player_eyebrow), activity.getString(R.string.text_playback_options), "")
            .choices(labels.toArray(new String[0]), n -> { if (host.player() != null) open(shown.get(n)); })
            .action(activity.getString(R.string.text_back), null));
    }
    private void open(PlayerOption option) {
        List<PlayerOption.Choice> choices = option.choices(host);
        String[] labels = new String[choices.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = (choices.get(i).selected ? "✓  " : "") + choices.get(i).label;
        prompter.show(new TvDialog(activity, activity.getString(R.string.text_playback_options), option.title(host), "")
            // The player may have been released (playback stopped) while the panel was open.
            .choices(labels, n -> { if (host.player() != null) choices.get(n).apply.run(); })
            .action(activity.getString(R.string.text_back), () -> open()));
    }
}
