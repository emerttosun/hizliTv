package tr.hizlitv.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import tr.hizlitv.media.LibraryIndex;
import tr.hizlitv.media.MediaEntry;

/** Serializes disk reads, writes and indexing off the UI thread. No background polling. */
final class LibraryRepository implements AutoCloseable {
    interface Result { void loaded(LibraryIndex index); void failed(); }
    private interface Operation { void run() throws Exception; }
    private final Context context;
    private final Store store;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean closed;
    LibraryRepository(Context c) { context = c.getApplicationContext(); store = new Store(context); }
    private void execute(Operation operation, Result result) {
        if (closed) return;
        io.execute(() -> {
            try {
                LegacyMigration.ensure(context); operation.run(); LibraryIndex index = new LibraryIndex(store.read());
                ui.post(() -> { if (!closed) result.loaded(index); });
            } catch (Exception failure) { android.util.Log.e("HizliTV.Library", "Operation failed: " + failure.getClass().getSimpleName()); ui.post(() -> { if (!closed) result.failed(); }); }
        });
    }
    void load(Result result) { execute(() -> { }, result); }
    void merge(List<MediaEntry> entries, Result result) { execute(() -> store.merge(entries), result); }
    void undo(Result result) { execute(store::restore, result); }
    void edit(MediaEntry target, boolean remove, Result result) {
        String key = Store.identity(target);
        execute(() -> {
            List<MediaEntry> entries = store.read(); boolean found = false;
            for (int i = 0; i < entries.size(); i++) if (key.equals(Store.identity(entries.get(i)))) {
                if (remove) entries.remove(i); else entries.get(i).favorite = !entries.get(i).favorite;
                found = true; break;
            }
            if (!found) throw new IOException("Entry changed before edit");
            store.write(entries, remove ? "library_remove" : "favorite_change");
        }, result);
    }
    void audit(String event, int count) {
        if (!closed) io.execute(() -> { try { store.audit(event, count); } catch (Exception ignored) { } });
    }
    @Override public void close() { closed = true; io.shutdownNow(); ui.removeCallbacksAndMessages(null); }
}
