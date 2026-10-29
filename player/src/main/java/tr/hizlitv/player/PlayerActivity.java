package tr.hizlitv.player;

import tr.hizlitv.media.MediaEntry;
import tr.hizlitv.media.M3u;
import tr.hizlitv.media.Urls;

import android.app.Activity;
import tr.hizlitv.ui.TvUi;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class PlayerActivity extends Activity {
    private final java.util.concurrent.ExecutorService audit = java.util.concurrent.Executors.newSingleThreadExecutor();
    private ExoPlayer player;
    private PlayerView view;
    private MediaEntry media;
    private long position;
    private boolean resume = true, alive;
    private String positionKey;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); alive = true;
        TvUi.prepare(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try {
            media = Store.entry(new JSONObject(getIntent().getStringExtra("media")));
            // Hash avoids keeping source credentials in preference keys or logs.
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(media.url.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder key = new StringBuilder(); for (byte b : digest) key.append(String.format(java.util.Locale.US, "%02x", b & 255)); positionKey = key.toString();
            position = state == null ? getSharedPreferences("playback", 0).getLong(positionKey, 0) : state.getLong("position", 0);
            resume = state == null || state.getBoolean("play", true);
            view = new PlayerView(this);
            // The video keeps its aspect ratio; uncovered space and loading frames stay black.
            view.setBackgroundColor(android.graphics.Color.BLACK);
            view.setShutterBackgroundColor(android.graphics.Color.BLACK);
            view.setShowSubtitleButton(true); view.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
            view.setControllerAutoShow(true); view.setControllerShowTimeoutMs(5000); setContentView(view); view.requestFocus();
        } catch (Exception e) { finish(); }
    }
    @Override protected void onStart() {
        super.onStart(); if (media == null || player != null) return;
        try {
            Map<String,String> headers = new HashMap<>(); if (!media.referer.isEmpty()) headers.put("Referer", M3u.safeHeader(media.referer));
            DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setUserAgent(media.userAgent.isEmpty() ? Resolver.AGENT : M3u.safeHeader(media.userAgent))
                .setConnectTimeoutMs(10000).setReadTimeoutMs(15000).setDefaultRequestProperties(headers);
            DefaultDataSource.Factory data = new DefaultDataSource.Factory(this, http);
            DefaultLoadControl buffer = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(10000, 30000, 1500, 3000).setTargetBufferBytes(24 * 1024 * 1024)
                .setPrioritizeTimeOverSizeThresholds(false).build();
            player = new ExoPlayer.Builder(this).setMediaSourceFactory(new DefaultMediaSourceFactory(data)).setLoadControl(buffer).build();
            player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                .setMaxVideoSize(1920, 1080).setPreferredAudioLanguage("tr").setPreferredTextLanguage("tr").build());
            player.addListener(new Player.Listener() {
                @Override public void onPlayerError(PlaybackException error) {
                    if (!alive || isFinishing()) return;
                    new AlertDialog.Builder(PlayerActivity.this).setTitle("Yayın oynatılamadı")
                        .setMessage("Kaynak bağlantısı süresi dolmuş, sunucu erişimi kısıtlı veya biçim bu cihazda desteklenmiyor olabilir. Hata: " + error.getErrorCodeName())
                        .setPositiveButton("Yeniden dene", (d, w) -> { if (player != null) { player.prepare(); player.play(); } })
                        .setNegativeButton("Geri", (d, w) -> finish()).show();
                }
                @Override public void onRenderedFirstFrame() {
                    audit.execute(() -> { try { new Store(getApplicationContext()).audit("video_first_frame", 1); } catch (Exception ignored) { } });
                }
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        int tracks = player.getCurrentTracks().getGroups().size();
                        audit.execute(() -> { try { new Store(getApplicationContext()).audit("player_ready", tracks); } catch (Exception ignored) { } });
                    }
                }
            });
            MediaItem.Builder item = new MediaItem.Builder().setUri(media.url);
            if (!media.mime.isEmpty()) item.setMimeType(media.mime);
            List<MediaItem.SubtitleConfiguration> subtitles = new ArrayList<>();
            for (MediaEntry.Subtitle s : media.subtitles) {
                MediaItem.SubtitleConfiguration.Builder sub = new MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(s.url))
                    .setMimeType(s.mime).setLabel(s.label).setRoleFlags(C.ROLE_FLAG_SUBTITLE);
                if (!s.language.isEmpty()) sub.setLanguage(s.language); subtitles.add(sub.build());
            }
            item.setSubtitleConfigurations(subtitles); view.setPlayer(player);
            player.setMediaItem(item.build()); if (position > 0) player.seekTo(position);
            player.setPlayWhenReady(resume); player.prepare();
        } catch (Exception e) { new AlertDialog.Builder(this).setMessage("Oynatıcı hazırlanamadı.").setPositiveButton("Geri", (d, w) -> finish()).show(); }
    }
    private void settings() {
        new AlertDialog.Builder(this).setTitle("Oynatma seçenekleri").setItems(new String[]{"Kalite", "Ses dili", "Altyazı"}, (d, n) -> tracks(n == 0 ? C.TRACK_TYPE_VIDEO : n == 1 ? C.TRACK_TYPE_AUDIO : C.TRACK_TYPE_TEXT)).show();
    }
    private void tracks(int type) {
        if (player == null) return;
        ArrayList<String> labels = new ArrayList<>(); ArrayList<TrackSelectionOverride> choices = new ArrayList<>();
        labels.add(type == C.TRACK_TYPE_TEXT ? "Altyazı kapalı" : "Otomatik"); choices.add(null);
        if (type == C.TRACK_TYPE_TEXT) { labels.add("Otomatik (Türkçe tercih)"); choices.add(null); }
        for (Tracks.Group g : player.getCurrentTracks().getGroups()) if (g.getType() == type)
            for (int i = 0; i < g.length; i++) {
                if (!g.isTrackSupported(i)) continue;
                androidx.media3.common.Format f = g.getTrackFormat(i);
                String label = f.label != null ? f.label : f.language != null ? f.language : "Seçenek " + (choices.size());
                if (type == C.TRACK_TYPE_VIDEO) label = f.height > 0 ? f.height + "p" : label;
                if (type == C.TRACK_TYPE_VIDEO && f.bitrate > 0) label += String.format(java.util.Locale.US, " · %.1f Mbps", f.bitrate / 1_000_000d);
                labels.add(label); choices.add(new TrackSelectionOverride(g.getMediaTrackGroup(), Collections.singletonList(i)));
            }
        new AlertDialog.Builder(this).setTitle(type == C.TRACK_TYPE_VIDEO ? "Kaynağın sunduğu kalite" : type == C.TRACK_TYPE_AUDIO ? "Ses" : "Altyazı")
            .setItems(labels.toArray(new String[0]), (d, n) -> {
                if (player == null) return;
                TrackSelectionParameters.Builder parameters = player.getTrackSelectionParameters().buildUpon().clearOverridesOfType(type);
                parameters.setTrackTypeDisabled(type, type == C.TRACK_TYPE_TEXT && n == 0);
                if (choices.get(n) != null) parameters.addOverride(choices.get(n)); player.setTrackSelectionParameters(parameters.build());
            }).setNegativeButton("Geri", null).show();
    }
    @Override public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_MENU && event.getRepeatCount() == 0) { settings(); return true; }
        if (key == KeyEvent.KEYCODE_BACK && view != null && view.isControllerFullyVisible()) { view.hideController(); return true; }
        return super.onKeyDown(key, event);
    }
    private void save() {
        if (player == null) return;
        position = player.isCurrentMediaItemLive() ? 0 : player.getPlaybackState() == Player.STATE_ENDED ? 0 : player.getCurrentPosition();
        resume = player.getPlayWhenReady(); getSharedPreferences("playback", 0).edit().putLong(positionKey, position).apply();
    }
    @Override protected void onSaveInstanceState(Bundle out) { save(); out.putLong("position", position); out.putBoolean("play", resume); super.onSaveInstanceState(out); }
    @Override protected void onStop() {
        save(); if (player != null) {
            androidx.media3.exoplayer.DecoderCounters counters = player.getVideoDecoderCounters();
            if (counters != null) {
                counters.ensureUpdated(); final int rendered = counters.renderedOutputBufferCount, dropped = counters.droppedBufferCount;
                audit.execute(() -> { try { Store store = new Store(getApplicationContext());
                    store.audit("video_frames_rendered", rendered); store.audit("video_frames_dropped", dropped);
                } catch (Exception ignored) { } });
            }
            view.setPlayer(null); player.release(); player = null;
        }
        super.onStop();
    }
    @Override protected void onDestroy() { alive = false; audit.shutdown(); super.onDestroy(); }
}
