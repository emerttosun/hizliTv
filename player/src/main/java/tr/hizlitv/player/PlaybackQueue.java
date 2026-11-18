package tr.hizlitv.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import tr.hizlitv.media.MediaEntry;

/**
 * In-process list the player can step through with channel keys. It is never put in an Intent
 * (a 5000-entry list would exceed the Binder limit); losing it with the process only disables zapping.
 */
final class PlaybackQueue {
    private static List<MediaEntry> entries = Collections.emptyList();
    private static int index;
    private PlaybackQueue() { }
    static synchronized void set(List<MediaEntry> list, int at) {
        entries = Collections.unmodifiableList(new ArrayList<>(list)); index = list.isEmpty() ? 0 : Math.max(0, Math.min(at, list.size() - 1));
    }
    static synchronized void clear() { entries = Collections.emptyList(); index = 0; }
    static synchronized List<MediaEntry> entries() { return entries; }
    static synchronized int index() { return index; }
}
