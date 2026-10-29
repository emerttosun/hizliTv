package tr.hizlitv.player;

import tr.hizlitv.ui.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import tr.hizlitv.media.LibraryIndex;
import tr.hizlitv.media.MediaEntry;
import tr.hizlitv.media.Urls;
import tr.hizlitv.ui.TvUi;
import tr.hizlitv.ui.TvDialog;

/** Embedded library. Disk/index work and source resolution never block remote navigation. */
public final class MainActivity extends tr.hizlitv.ui.LocalizedActivity implements LibraryScreen.Actions {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean();
    private LibraryIndex library = new LibraryIndex(Collections.emptyList());
    private LibraryRepository repository;
    private LibraryScreen screen;
    private PhoneCompanion phone;
    private TvDialog sourceDialog;
    private volatile boolean alive, started, libraryBusy = true, libraryReady;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); alive = true; TvUi.prepare(this); repository = new LibraryRepository(this);
        screen = new LibraryScreen(this, this); setContentView(screen); screen.working(true);
        phone = new PhoneCompanion(this, this::submit, new PhoneCompanion.Events() {
            public void status(String text) { screen.status(text); }
            public void audit(String event) { repository.audit(event, 0); }
        });
        repository.load(result(getString(R.string.text_the_library_could_not_be_read_old_player_records_are_prese)));
    }
    private LibraryRepository.Result result(String error) {
        return new LibraryRepository.Result() {
            public void loaded(LibraryIndex index) {
                library = index; libraryBusy = false; screen.showLibrary(index); screen.working(false);
                if (!libraryReady) { libraryReady = true; if (started) phone.onStart(); }
            }
            public void failed() { libraryBusy = false; screen.working(!libraryReady); screen.status(error); }
        };
    }
    private boolean beginEdit() {
        if (!libraryReady || libraryBusy) return false;
        libraryBusy = true; screen.working(true); return true;
    }
    private void merge(List<MediaEntry> entries) {
        if (beginEdit()) repository.merge(entries, result(getString(R.string.text_could_not_save_existing_records_are_preserved)));
    }
    @Override public void home() { finish(); }
    @Override public void phone() { if (libraryReady && !libraryBusy) phone.open(); }
    @Override public void input() {
        if (!libraryReady || libraryBusy) return;
        EditText field = new EditText(this); field.setTextSize(15); field.setHint(getString(R.string.link_example)); field.setMaxLines(5);
        new AlertDialog.Builder(this).setTitle(getString(R.string.text_link_or_channel_list)).setView(field)
            .setPositiveButton(getString(R.string.text_find_sources), (d, w) -> submit(field.getText().toString())).setNegativeButton(getString(R.string.text_cancel), null).show();
    }
    @Override public void open(MediaEntry entry) { if (entry.sourcePage.isEmpty()) confirm(entry); else submit(entry.sourcePage); }
    private boolean submit(String input) {
        if (!alive || !started || !libraryReady || libraryBusy || !busy.compareAndSet(false, true)) return false;
        ui.post(() -> { if (alive) screen.status(getString(R.string.text_finding_sources)); });
        worker.execute(() -> {
            try {
                Resolver.Result result = new Resolver(SourceAdapters.defaults(), getString(R.string.text_live_stream_video), getString(R.string.text_subtitles)).resolve(input);
                ui.post(() -> { if (alive && started) { phone.closeDialog(); screen.status(getString(R.string.source_ready_status)); results(result); } });
            } catch (Exception failure) {
                String message = failure.getMessage() == null ? getString(R.string.text_the_source_could_not_be_read) : tr.hizlitv.ui.I18n.error(this, failure);
                ui.post(() -> { if (alive && started) { phone.closeDialog(); screen.status(getString(R.string.text_the_link_could_not_be_opened));
                    showSource(new TvDialog(this, getString(R.string.text_h_zl_tv_player_link), getString(R.string.text_could_not_open_link), message).action(getString(R.string.text_ok), null)); } });
            } finally { busy.set(false); }
        });
        return true;
    }
    private void results(Resolver.Result result) {
        if (result.playlist) {
            showSource(new TvDialog(this, getString(R.string.text_h_zl_tv_player_channel_list), getResources().getQuantityString(R.plurals.channels_found, result.items.size(), result.items.size()),
                getString(R.string.text_add_this_channel_list_to_your_library_you_can_undo_the_lat))
                .action(getString(R.string.add_playlist), () -> merge(result.items)).action(getString(R.string.text_cancel), null)); return;
        }
        if (result.items.size() == 1) { confirm(result.items.get(0)); return; }
        String[] names = new String[result.items.size()];
        for (int i = 0; i < names.length; i++) names[i] = result.items.get(i).title + " · " + Urls.host(result.items.get(i).url);
        showSource(new TvDialog(this, getString(R.string.text_h_zl_tv_player_source_found), getString(R.string.text_choose_a_source), "")
            .choices(names, n -> confirm(result.items.get(n))).action(getString(R.string.text_back), null));
    }
    private void showSource(TvDialog dialog) {
        if (sourceDialog != null) sourceDialog.dismiss(); sourceDialog = dialog;
        dialog.setOnDismissListener(d -> { if (sourceDialog == dialog) sourceDialog = null; }); dialog.show();
    }
    private void confirm(MediaEntry entry) {
        showSource(new TvDialog(this, getString(R.string.text_h_zl_tv_player_source_found), entry.title,
            getString(R.string.text_source) + Urls.host(entry.url) + (entry.subtitles.isEmpty() ? "" : "\n" + entry.subtitles.size() + getString(R.string.text_subtitle_options)) + getString(R.string.text_ready_to_watch))
            .action(getString(R.string.text_play), () -> {
                try { repository.audit("play_request", 1); startActivity(new Intent(this, PlayerActivity.class).putExtra("media", Store.json(entry).toString())); }
                catch (Exception failure) { screen.status(getString(R.string.text_the_player_could_not_be_opened)); }
            })
            .action(getString(R.string.text_add_to_library), () -> merge(Collections.singletonList(entry)))
            .action(getString(R.string.text_cancel), null));
    }
    @Override public void edit(MediaEntry entry) {
        new AlertDialog.Builder(this).setTitle(entry.title)
            .setItems(new String[]{entry.favorite ? getString(R.string.text_remove_favorite) : getString(R.string.text_add_favorite), getString(R.string.text_remove_entry)}, (d, n) -> {
                if (beginEdit()) repository.edit(entry, n == 1, result(getString(R.string.text_changes_could_not_be_saved)));
            }).setNegativeButton(getString(R.string.text_back), null).show();
    }
    @Override public void groups() {
        ArrayList<String> labels = new ArrayList<>(); labels.add(getString(R.string.text_all_groups)); labels.addAll(library.groups());
        new AlertDialog.Builder(this).setTitle(getString(R.string.text_groups)).setItems(labels.toArray(new String[0]), (d, n) -> screen.group(n == 0 ? "" : labels.get(n))).show();
    }
    @Override public void undo() { if (beginEdit()) repository.undo(result(getString(R.string.text_nothing_to_undo_or_the_backup_could_not_be_read))); }
    @Override protected void onStart() { super.onStart(); started = true; if (libraryReady) phone.onStart(); }
    @Override protected void onStop() { started = false; if (sourceDialog != null) sourceDialog.dismiss(); phone.stop(); super.onStop(); }
    @Override protected void onDestroy() { alive = false; phone.stop(); repository.close(); worker.shutdownNow(); ui.removeCallbacksAndMessages(null); super.onDestroy(); }
}
