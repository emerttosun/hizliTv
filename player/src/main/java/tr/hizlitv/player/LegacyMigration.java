package tr.hizlitv.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/** One-time, same-owner migration. Runs only on the repository's disk worker.
 * The untouched archive remains in private storage before any old app is removed. */
final class LegacyMigration {
    private static boolean checked;
    private static final int TOTAL_LIMIT = 32 * 1024 * 1024;
    static synchronized void ensure(Context c) throws Exception {
        if (checked) return;
        SharedPreferences state = c.getSharedPreferences("player_migration", 0);
        if (state.getBoolean("complete", false)) { checked = true; return; }
        try { c.getPackageManager().getApplicationInfo("tr.hizlitv.player", 0); }
        catch (PackageManager.NameNotFoundException absent) { checked = true; return; }
        if (c.getPackageManager().resolveContentProvider("tr.hizlitv.player.migration", 0) == null)
            throw new IOException("A signed migration bridge is required for the old Player; records were not changed.");
        if (new File(c.getFilesDir(), "library.json").exists() && !state.getBoolean("in_progress", false))
            throw new IOException("Manual migration is required to preserve both libraries.");
        AtomicFile archive = new AtomicFile(new File(c.getFilesDir(), "legacy-player-backup.zip"));
        FileOutputStream saved = archive.startWrite();
        try (InputStream in = c.getContentResolver().openInputStream(Uri.parse("content://tr.hizlitv.player.migration/snapshot"))) {
            if (in == null) throw new IOException("The migration source could not be opened.");
            copy(in, saved, TOTAL_LIMIT); archive.finishWrite(saved);
        } catch (Exception failure) { archive.failWrite(saved); throw failure; }
        File staging = new File(c.getCacheDir(), "player-migration"); if (!staging.exists() && !staging.mkdirs()) throw new IOException("Staging unavailable");
        Set<String> seen = new HashSet<>(); long total = 0;
        Set<String> allowed = new HashSet<>(Arrays.asList("library.json", "library-undo.json", "operations.jsonl", "phone.json", "playback.json", "manifest.json"));
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(archive.getBaseFile()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || !allowed.contains(name) || !seen.add(name)) throw new IOException("Invalid migration entry");
                int limit = name.startsWith("library") ? 8_000_000 : name.equals("operations.jsonl") ? 4_000_000 : 2_000_000;
                try (FileOutputStream out = new FileOutputStream(new File(staging, name))) { total += copy(zip, out, limit); }
                if (total > TOTAL_LIMIT) throw new IOException("Migration limit"); zip.closeEntry();
            }
        }
        if (!seen.containsAll(Arrays.asList("phone.json", "playback.json", "manifest.json"))) throw new IOException("Incomplete migration snapshot");
        JSONObject manifest = json(new File(staging, "manifest.json"));
        if (manifest.getInt("schema") != 1 || !manifest.getBoolean("complete")) throw new IOException("Incomplete migration snapshot");
        for (String name : Arrays.asList("library.json", "library-undo.json")) if (seen.contains(name)) {
            JSONArray entries = new JSONArray(new String(read(new File(staging, name), 8_000_000), StandardCharsets.UTF_8));
            if (entries.length() > 5000) throw new IOException("Library entry limit");
            for (int i = 0; i < entries.length(); i++) Store.entry(entries.getJSONObject(i));
        }
        JSONObject phone = json(new File(staging, "phone.json")); String token = phone.optString("token", "");
        int port = phone.optInt("port", 8198);
        if (!token.isEmpty() && (!token.matches("[a-f0-9]{48}") || port < 1 || port > 65535)) throw new IOException("Invalid pairing snapshot");
        JSONObject positions = json(new File(staging, "playback.json"));
        for (Iterator<String> keys = positions.keys(); keys.hasNext();) {
            String key = keys.next(); if (!key.matches("[a-f0-9]{64}") || positions.getLong(key) < 0) throw new IOException("Invalid playback snapshot");
        }
        if (!state.edit().putBoolean("in_progress", true).commit()) throw new IOException("Migration marker not saved");
        for (String name : Arrays.asList("library.json", "library-undo.json", "operations.jsonl")) if (seen.contains(name)) {
            AtomicFile target = new AtomicFile(new File(c.getFilesDir(), name)); FileOutputStream out = target.startWrite();
            try (InputStream in = new FileInputStream(new File(staging, name))) { copy(in, out, 8_000_000); target.finishWrite(out); }
            catch (Exception failure) { target.failWrite(out); throw failure; }
        }
        SharedPreferences.Editor pairing = c.getSharedPreferences("phone", 0).edit().clear();
        if (!token.isEmpty()) pairing.putString("token", token).putInt("port", port);
        if (!pairing.commit()) throw new IOException("Pairing not saved");
        SharedPreferences.Editor playback = c.getSharedPreferences("playback", 0).edit().clear();
        for (Iterator<String> keys = positions.keys(); keys.hasNext();) { String key = keys.next(); playback.putLong(key, positions.getLong(key)); }
        if (!playback.commit()) throw new IOException("Playback state not saved");
        int count = new Store(c).read().size();
        boolean preserved = token.equals(c.getSharedPreferences("phone", 0).getString("token", "")) && (token.isEmpty() || port == c.getSharedPreferences("phone", 0).getInt("port", -1));
        if (!preserved) throw new IOException("Pairing verification failed");
        JSONObject result = new JSONObject().put("library_count", count).put("position_count", positions.length())
            .put("phone_preserved", true).put("private_backup_bytes", archive.getBaseFile().length()).put("complete", true);
        try (FileOutputStream out = c.openFileOutput("player-migration-result.json", Context.MODE_PRIVATE)) { out.write(result.toString().getBytes(StandardCharsets.UTF_8)); }
        if (!state.edit().putBoolean("complete", true).putBoolean("in_progress", false).commit()) throw new IOException("Final migration marker not saved");
        checked = true; android.util.Log.i("HizliTV.Migration", result.toString());
        for (String name : seen) new File(staging, name).delete(); staging.delete();
    }
    private static long copy(InputStream in, OutputStream out, int limit) throws IOException {
        byte[] bytes = new byte[8192]; int n; long count = 0;
        while ((n = in.read(bytes)) != -1) { count += n; if (count > limit) throw new IOException("Migration size limit"); out.write(bytes, 0, n); }
        return count;
    }
    private static byte[] read(File file, int limit) throws IOException {
        try (InputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) { copy(in, out, limit); return out.toByteArray(); }
    }
    private static JSONObject json(File file) throws Exception { return new JSONObject(new String(read(file, 2_000_000), StandardCharsets.UTF_8)); }
}
