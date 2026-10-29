package tr.hizlitv.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

/** Normalizes titles once per library snapshot, rather than on every key press.
 * Callers treat entries as immutable and replace the index after an edit. */
public final class LibraryIndex {
    private static String normalized(String value) { return value.toLowerCase(Locale.ROOT).replace('ı', 'i').replace("\u0307", ""); }
    private final List<MediaEntry> entries;
    private final List<String> titles;
    private final SortedSet<String> groups;
    public LibraryIndex(List<MediaEntry> library) {
        entries = Collections.unmodifiableList(new ArrayList<>(library));
        titles = new ArrayList<>(entries.size()); TreeSet<String> names = new TreeSet<>();
        for (MediaEntry entry : entries) {
            titles.add(normalized(entry.title)); if (!entry.group.isEmpty()) names.add(entry.group);
        }
        groups = Collections.unmodifiableSortedSet(names);
    }
    public List<MediaEntry> entries() { return entries; }
    public SortedSet<String> groups() { return groups; }
    public List<MediaEntry> filter(boolean favorites, String group, String query) {
        String search = normalized(query.trim()); List<MediaEntry> result = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            MediaEntry entry = entries.get(i);
            if (favorites && !entry.favorite || !group.isEmpty() && !group.equals(entry.group)) continue;
            if (titles.get(i).contains(search)) result.add(entry);
        }
        return result;
    }
}
