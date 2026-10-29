package tr.hizlitv.media;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Filtering does not mutate, persist or resolve entries. Safe to reuse outside Android. */
public final class LibraryQuery {
    private static final Locale TURKISH = new Locale("tr", "TR");
    private LibraryQuery() { }
    public static List<MediaEntry> filter(List<MediaEntry> library, boolean favorites, String group, String query) {
        return new LibraryIndex(library).filter(favorites, group, query);
    }
}
