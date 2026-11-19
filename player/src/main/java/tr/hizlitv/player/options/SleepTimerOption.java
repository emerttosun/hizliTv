package tr.hizlitv.player.options;

import java.util.ArrayList;
import java.util.List;
import tr.hizlitv.ui.R;

/** Pauses playback after a chosen time; the Host owns the timer so it survives channel changes. */
final class SleepTimerOption implements PlayerOption {
    static final int[] MINUTES = {0, 15, 30, 60, 90};
    /** Whole minutes still to go, rounded up so "1 min left" never shows while seconds remain beyond it. */
    static int minutesLeft(long remainingMs) { return remainingMs <= 0 ? 0 : (int) ((remainingMs + 59_999) / 60_000); }
    @Override public boolean available(Host host) { return true; }
    @Override public String title(Host host) { return host.context().getString(R.string.player_sleep); }
    @Override public String value(Host host) {
        int left = minutesLeft(host.sleepRemainingMs());
        return left == 0 ? host.context().getString(R.string.player_sleep_off) : host.context().getString(R.string.player_sleep_left, left);
    }
    @Override public List<Choice> choices(Host host) {
        List<Choice> out = new ArrayList<>(); boolean running = host.sleepRemainingMs() > 0;
        for (int minutes : MINUTES) {
            String label = minutes == 0 ? host.context().getString(R.string.player_sleep_off) : host.context().getString(R.string.player_minutes, minutes);
            boolean selected = minutes == 0 ? !running : running && host.sleepMinutes() == minutes;
            out.add(new Choice(label, selected, () -> host.sleepAfter(minutes)));
        }
        return out;
    }
}
