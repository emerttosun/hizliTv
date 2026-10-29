package tr.hizlitv.player;

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
public final class MainActivity extends Activity implements LibraryScreen.Actions {
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
        repository.load(result("Kütüphane okunamadı. Eski Player kayıtları korunuyor; aktarımı kontrol et."));
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
        if (beginEdit()) repository.merge(entries, result("Kaydedilemedi; mevcut kayıtlar korunuyor."));
    }
    @Override public void home() { finish(); }
    @Override public void phone() { if (libraryReady && !libraryBusy) phone.open(); }
    @Override public void input() {
        if (!libraryReady || libraryBusy) return;
        EditText field = new EditText(this); field.setTextSize(15); field.setHint("https://… veya #EXTM3U…"); field.setMaxLines(5);
        new AlertDialog.Builder(this).setTitle("Bağlantı veya kanal listesi").setView(field)
            .setPositiveButton("Kaynakları bul", (d, w) -> submit(field.getText().toString())).setNegativeButton("Vazgeç", null).show();
    }
    @Override public void open(MediaEntry entry) { if (entry.sourcePage.isEmpty()) confirm(entry); else submit(entry.sourcePage); }
    private boolean submit(String input) {
        if (!alive || !started || !libraryReady || libraryBusy || !busy.compareAndSet(false, true)) return false;
        ui.post(() -> { if (alive) screen.status("Kaynaklar aranıyor…"); });
        worker.execute(() -> {
            try {
                Resolver.Result result = new Resolver().resolve(input);
                ui.post(() -> { if (alive && started) { phone.closeDialog(); screen.status("Kaynak bulundu; TV’de onayla."); results(result); } });
            } catch (Exception failure) {
                String message = failure.getMessage() == null ? "Kaynak okunamadı." : failure.getMessage();
                ui.post(() -> { if (alive && started) { phone.closeDialog(); screen.status("Bağlantı açılamadı.");
                    showSource(new TvDialog(this, "HızlıTV Player · Bağlantı", "Bağlantı açılamadı", message).action("Tamam", null)); } });
            } finally { busy.set(false); }
        });
        return true;
    }
    private void results(Resolver.Result result) {
        if (result.playlist) {
            showSource(new TvDialog(this, "HızlıTV Player · Kanal listesi", result.items.size() + " kanal bulundu",
                "Kanal listesi kütüphanene eklensin mi? Son değişikliği geri alabilirsin.")
                .action("Listeyi ekle", () -> merge(result.items)).action("Vazgeç", null)); return;
        }
        if (result.items.size() == 1) { confirm(result.items.get(0)); return; }
        String[] names = new String[result.items.size()];
        for (int i = 0; i < names.length; i++) names[i] = result.items.get(i).title + " · " + Urls.host(result.items.get(i).url);
        showSource(new TvDialog(this, "HızlıTV Player · Kaynak bulundu", "Bir kaynak seç", "")
            .choices(names, n -> confirm(result.items.get(n))).action("Geri", null));
    }
    private void showSource(TvDialog dialog) {
        if (sourceDialog != null) sourceDialog.dismiss(); sourceDialog = dialog;
        dialog.setOnDismissListener(d -> { if (sourceDialog == dialog) sourceDialog = null; }); dialog.show();
    }
    private void confirm(MediaEntry entry) {
        showSource(new TvDialog(this, "HızlıTV Player · Kaynak bulundu", entry.title,
            "Kaynak: " + Urls.host(entry.url) + (entry.subtitles.isEmpty() ? "" : "\n" + entry.subtitles.size() + " altyazı seçeneği") + "\nİzlemeye hazır.")
            .action("Oynat", () -> {
                try { repository.audit("play_request", 1); startActivity(new Intent(this, PlayerActivity.class).putExtra("media", Store.json(entry).toString())); }
                catch (Exception failure) { screen.status("Oynatıcı açılamadı."); }
            })
            .action("Kütüphaneye ekle", () -> merge(Collections.singletonList(entry)))
            .action("Vazgeç", null));
    }
    @Override public void edit(MediaEntry entry) {
        new AlertDialog.Builder(this).setTitle(entry.title)
            .setItems(new String[]{entry.favorite ? "Favoriden çıkar" : "Favoriye ekle", "Kaydı kaldır"}, (d, n) -> {
                if (beginEdit()) repository.edit(entry, n == 1, result("Değişiklik kaydedilemedi."));
            }).setNegativeButton("Geri", null).show();
    }
    @Override public void groups() {
        ArrayList<String> labels = new ArrayList<>(); labels.add("Tüm gruplar"); labels.addAll(library.groups());
        new AlertDialog.Builder(this).setTitle("Gruplar").setItems(labels.toArray(new String[0]), (d, n) -> screen.group(n == 0 ? "" : labels.get(n))).show();
    }
    @Override public void undo() { if (beginEdit()) repository.undo(result("Geri alınacak değişiklik yok veya yedek okunamadı.")); }
    @Override protected void onStart() { super.onStart(); started = true; if (libraryReady) phone.onStart(); }
    @Override protected void onStop() { started = false; if (sourceDialog != null) sourceDialog.dismiss(); phone.stop(); super.onStop(); }
    @Override protected void onDestroy() { alive = false; phone.stop(); repository.close(); worker.shutdownNow(); ui.removeCallbacksAndMessages(null); super.onDestroy(); }
}
