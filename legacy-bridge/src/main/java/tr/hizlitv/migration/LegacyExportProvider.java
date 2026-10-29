package tr.hizlitv.migration;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.json.JSONObject;

/** Temporary upgrade of the former companion. No UI, network or privileged permission.
 * Only another app signed by the same owner can read the bounded migration stream. */
public final class LegacyExportProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    private void requireOwner() {
        if (getContext().getPackageManager().checkSignatures(Binder.getCallingUid(), Process.myUid()) != PackageManager.SIGNATURE_MATCH)
            throw new SecurityException("Matching signing identity required");
    }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        requireOwner();
        if (!"r".equals(mode) || !"/snapshot".equals(uri.getPath())) throw new FileNotFoundException("Unknown migration resource");
        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            new Thread(() -> {
                try (ZipOutputStream zip = new ZipOutputStream(new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]))) {
                    file(zip, "library.json", 8_000_000);
                    file(zip, "library-undo.json", 8_000_000);
                    file(zip, "operations.jsonl", 4_000_000);
                    json(zip, "phone.json", new JSONObject(getContext().getSharedPreferences("phone", 0).getAll()));
                    JSONObject positions = new JSONObject();
                    for (Map.Entry<String,?> e : getContext().getSharedPreferences("PlayerActivity", 0).getAll().entrySet())
                        if (e.getKey().matches("[a-f0-9]{64}") && e.getValue() instanceof Number) positions.put(e.getKey(), e.getValue());
                    json(zip, "playback.json", positions);
                    json(zip, "manifest.json", new JSONObject().put("schema", 1).put("complete", true));
                } catch (Exception failure) {
                    android.util.Log.e("HizliTV.Migration", "Snapshot export failed: " + failure.getClass().getSimpleName());
                    try { pipe[1].close(); } catch (Exception ignored) { }
                }
            }, "PlayerSnapshot").start();
            return pipe[0];
        } catch (Exception failure) { throw new FileNotFoundException("Migration pipe unavailable"); }
    }
    private void file(ZipOutputStream zip, String name, long max) throws Exception {
        File source = new File(getContext().getFilesDir(), name); if (!source.exists()) return;
        if (source.length() > max) throw new java.io.IOException("Snapshot file limit");
        zip.putNextEntry(new ZipEntry(name));
        try (FileInputStream in = new FileInputStream(source)) { byte[] bytes = new byte[8192]; int n; long size = 0;
            while ((n = in.read(bytes)) != -1) { size += n; if (size > max) throw new java.io.IOException("Snapshot limit"); zip.write(bytes, 0, n); }
        }
        zip.closeEntry();
    }
    private void json(ZipOutputStream zip, String name, JSONObject json) throws Exception {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 2_000_000) throw new java.io.IOException("Preference snapshot limit");
        zip.putNextEntry(new ZipEntry(name)); zip.write(bytes); zip.closeEntry();
    }
    @Override public String getType(Uri uri) { requireOwner(); return "application/zip"; }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { requireOwner(); throw new UnsupportedOperationException(); }
    @Override public Uri insert(Uri u, ContentValues v) { requireOwner(); throw new UnsupportedOperationException(); }
    @Override public int delete(Uri u, String s, String[] a) { requireOwner(); throw new UnsupportedOperationException(); }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { requireOwner(); throw new UnsupportedOperationException(); }
}
