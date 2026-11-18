package tr.hizlitv.player;

import tr.hizlitv.ui.R;
import tr.hizlitv.media.MediaEntry;
import tr.hizlitv.media.M3u;
import tr.hizlitv.media.Urls;

import tr.hizlitv.ui.TvDialog;
import tr.hizlitv.ui.TvUi;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/**
 * Remote-first playback. Keys: OK play/pause, ◀ ▶ seek (faster while held), ▲ ▼ / CH± previous/next
 * entry of the list it was opened from, Menu options, Back hides the chrome then leaves.
 */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class PlayerActivity extends tr.hizlitv.ui.LocalizedActivity {
    private static final long RESUME_THRESHOLD_MS = 60_000, ZAP_DELAY_MS = 700;
    private static final int[] RESIZE_MODES = {AspectRatioFrameLayout.RESIZE_MODE_FIT, AspectRatioFrameLayout.RESIZE_MODE_ZOOM, AspectRatioFrameLayout.RESIZE_MODE_FILL};
    private static final float[] SPEEDS = {0.75f, 1f, 1.25f, 1.5f, 2f};
    private final java.util.concurrent.ExecutorService audit = java.util.concurrent.Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private ExoPlayer player;
    private PlayerView view;
    private PlayerOverlay overlay;
    private TvDialog prompt;
    private MediaEntry media;
    private List<MediaEntry> queue = Collections.emptyList();
    private int index, pendingIndex = -1, reconnects;
    private long position;
    private boolean resume = true, alive, restored, everReady, askingResume;
    private float speed = 1f;
    private String positionKey;
    private final Runnable zap = this::applyZap;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); alive = true;
        TvUi.prepare(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        try {
            media = Store.entry(new JSONObject(getIntent().getStringExtra("media")));
            if (getIntent().getBooleanExtra("queue", false)) {
                List<MediaEntry> list = PlaybackQueue.entries(); int at = PlaybackQueue.index();
                // Use the list only if it still matches what was opened.
                if (at < list.size() && list.get(at).url.equals(media.url)) { queue = list; index = at; }
            }
            positionKey = key(media);
            restored = state != null;
            position = state == null ? getSharedPreferences("playback", 0).getLong(positionKey, 0) : state.getLong("position", 0);
            resume = state == null || state.getBoolean("play", true);
            view = new PlayerView(this);
            // The video keeps its aspect ratio by default; uncovered space and loading frames stay black.
            view.setBackgroundColor(android.graphics.Color.BLACK); view.setShutterBackgroundColor(android.graphics.Color.BLACK);
            view.setUseController(false); view.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
            view.setResizeMode(RESIZE_MODES[Math.max(0, Math.min(RESIZE_MODES.length - 1, getSharedPreferences("player", 0).getInt("resize", 0)))]);
            overlay = new PlayerOverlay(this);
            FrameLayout root = new FrameLayout(this); root.setBackgroundColor(android.graphics.Color.BLACK);
            root.addView(view, new FrameLayout.LayoutParams(-1, -1)); root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
            setContentView(root);
        } catch (Exception e) { finish(); }
    }
    /** Hash avoids keeping source credentials in preference keys or logs. */
    private static String key(MediaEntry entry) throws Exception {
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(entry.url.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder key = new StringBuilder(); for (byte b : digest) key.append(String.format(java.util.Locale.US, "%02x", b & 255)); return key.toString();
    }
    @Override protected void onStart() {
        super.onStart(); if (media == null || player != null) return;
        start(!restored); restored = true;
    }
    /** Builds a player for {@link #media}; headers differ per entry, so channel changes rebuild it. */
    private void start(boolean askResume) {
        try {
            everReady = false; reconnects = 0;
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
                .setMaxVideoSize(1920, 1080).setPreferredAudioLanguage(tr.hizlitv.ui.Language.code(this)).setPreferredTextLanguage(tr.hizlitv.ui.Language.code(this)).build());
            player.addListener(new Player.Listener() {
                @Override public void onPlayerError(PlaybackException error) { failed(error); }
                @Override public void onRenderedFirstFrame() {
                    audit.execute(() -> { try { new Store(getApplicationContext()).audit("video_first_frame", 1); } catch (Exception ignored) { } });
                }
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        if (reconnects > 0) overlay.clearMessage();
                        everReady = true; reconnects = 0;
                        int tracks = player.getCurrentTracks().getGroups().size();
                        audit.execute(() -> { try { new Store(getApplicationContext()).audit("player_ready", tracks); } catch (Exception ignored) { } });
                    }
                    if (state == Player.STATE_ENDED) overlay.show();
                }
                // Pausing keeps the chrome up; short rebuffering does not pop it open.
                @Override public void onIsPlayingChanged(boolean playing) { if (playing) overlay.scheduleHide(); else if (player != null && !player.getPlayWhenReady()) overlay.show(); }
            });
            MediaItem.Builder item = new MediaItem.Builder().setUri(media.url);
            if (!media.mime.isEmpty()) item.setMimeType(media.mime);
            List<MediaItem.SubtitleConfiguration> subtitles = new ArrayList<>();
            for (MediaEntry.Subtitle s : media.subtitles) {
                MediaItem.SubtitleConfiguration.Builder sub = new MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(s.url))
                    .setMimeType(s.mime).setLabel(s.label).setRoleFlags(C.ROLE_FLAG_SUBTITLE);
                if (!s.language.isEmpty()) sub.setLanguage(s.language); subtitles.add(sub.build());
            }
            item.setSubtitleConfigurations(subtitles); view.setPlayer(player); overlay.attach(player); describe();
            player.setMediaItem(item.build()); player.setPlaybackParameters(new PlaybackParameters(speed));
            if (askResume && position >= RESUME_THRESHOLD_MS) {
                // Ask instead of silently jumping: prepare paused so the answer starts instantly.
                player.setPlayWhenReady(false); player.prepare(); askResume();
            } else {
                if (position > 0) player.seekTo(position);
                player.setPlayWhenReady(resume); player.prepare();
            }
            overlay.show();
        } catch (Exception e) {
            show(new TvDialog(this, getString(R.string.player_eyebrow), getString(R.string.text_cannot_play_this_stream), getString(R.string.text_the_player_could_not_be_prepared))
                .action(getString(R.string.text_back), this::finish));
        }
    }
    private void describe() {
        ArrayList<String> info = new ArrayList<>();
        if (!media.group.isEmpty()) info.add(media.group);
        String host = Urls.host(media.url); if (host != null && !host.isEmpty()) info.add(host);
        if (queue.size() > 1) info.add(getString(R.string.player_queue_position, index + 1, queue.size()));
        String keys = getString(R.string.player_hint);
        if (queue.size() > 1) keys += "  ·  " + getString(R.string.player_hint_channels);
        overlay.describe(media.title, android.text.TextUtils.join("  ·  ", info), keys);
    }
    private void askResume() {
        askingResume = true;
        TvDialog dialog = new TvDialog(this, getString(R.string.player_eyebrow), getString(R.string.player_resume_title), getString(R.string.player_resume_message, PlayerOverlay.format(position)))
            .action(getString(R.string.player_resume), () -> resumeFrom(position))
            .action(getString(R.string.player_start_over), () -> resumeFrom(0));
        // Back on the question keeps the familiar behaviour: continue where it stopped.
        dialog.setOnCancelListener(d -> resumeFrom(position)); show(dialog);
    }
    private void resumeFrom(long at) { askingResume = false; if (player != null) { player.seekTo(at); player.play(); } }
    private void show(TvDialog dialog) {
        if (prompt != null) prompt.dismiss(); prompt = dialog;
        dialog.setOnDismissListener(d -> { if (prompt == dialog) prompt = null; }); dialog.show();
    }

    // ---- Errors and reconnecting -------------------------------------------------------------
    private void failed(PlaybackException error) {
        if (!alive || isFinishing() || player == null) return;
        int code = error.errorCode;
        if (code == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) { player.seekToDefaultPosition(); player.prepare(); return; }
        // A stream that already played and then dropped is retried quietly a few times before asking.
        if (everReady && network(code) && reconnects < 3) {
            reconnects++; overlay.show(); overlay.message(getString(R.string.player_reconnecting), 0);
            ui.postDelayed(() -> { if (player != null) { player.prepare(); player.play(); } }, 2000L * reconnects); return;
        }
        overlay.clearMessage();
        TvDialog dialog = new TvDialog(this, getString(R.string.player_eyebrow), getString(R.string.text_cannot_play_this_stream),
            getString(reason(code)) + "\n" + getString(R.string.player_error_code, error.getErrorCodeName()))
            .action(getString(R.string.text_retry), () -> { if (player != null) { reconnects = 0; player.prepare(); player.play(); } });
        if (queue.size() > 1) dialog.action(getString(R.string.player_next), () -> { pendingIndex = (index + 1) % queue.size(); applyZap(); });
        show(dialog.action(getString(R.string.text_back), this::finish));
    }
    private static boolean network(int code) { return PlaybackErrors.network(code); }
    private static int reason(int code) {
        switch (PlaybackErrors.kind(code)) {
            case HTTP: return R.string.player_error_http;
            case NETWORK: return R.string.player_error_network;
            case DRM: return R.string.player_error_drm;
            case FORMAT: return R.string.player_error_format;
            default: return R.string.player_error_generic;
        }
    }

    // ---- Keys ------------------------------------------------------------------------------
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (player == null || event.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event);
        int repeat = event.getRepeatCount();
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_MENU: if (repeat == 0) settings(); return true;
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER: case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: case KeyEvent.KEYCODE_SPACE:
                if (repeat == 0) toggle(); return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY: player.play(); overlay.show(); return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE: player.pause(); overlay.show(); return true;
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_MEDIA_REWIND: seek(-1, repeat); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD: seek(1, repeat); return true;
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_CHANNEL_DOWN: case KeyEvent.KEYCODE_MEDIA_PREVIOUS: step(-1); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN: case KeyEvent.KEYCODE_CHANNEL_UP: case KeyEvent.KEYCODE_MEDIA_NEXT: step(1); return true;
            case KeyEvent.KEYCODE_BACK:
                if (overlay.shown() && player.isPlaying()) { overlay.hideNow(); return true; }
                return super.dispatchKeyEvent(event);
            default: return super.dispatchKeyEvent(event);
        }
    }
    private void toggle() {
        if (player.getPlaybackState() == Player.STATE_ENDED) { player.seekTo(0); player.play(); }
        else if (player.getPlayWhenReady()) player.pause(); else player.play();
        overlay.show();
    }
    private boolean seekable() { return !player.isCurrentMediaItemLive() && player.getDuration() != C.TIME_UNSET && player.isCurrentMediaItemSeekable(); }
    /** 10 s steps, 30 s after holding briefly, 60 s after holding longer. */
    private void seek(int direction, int repeat) {
        overlay.show(); if (!seekable()) return;
        long step = repeat < 6 ? 10_000 : repeat < 20 ? 30_000 : 60_000;
        long target = Math.max(0, Math.min(player.getDuration(), player.getCurrentPosition() + direction * step));
        player.seekTo(target); overlay.message((direction > 0 ? "+" : "−") + step / 1000 + " " + getString(R.string.player_seconds), 1500);
    }
    /** Previous/next entry; repeated presses only move the target until the remote is released. */
    private void step(int delta) {
        if (queue.size() < 2) { overlay.show(); return; }
        int from = pendingIndex < 0 ? index : pendingIndex;
        pendingIndex = (from + delta + queue.size()) % queue.size();
        MediaEntry target = queue.get(pendingIndex);
        overlay.show(); overlay.message((pendingIndex + 1) + "  ·  " + target.title, ZAP_DELAY_MS + 600);
        ui.removeCallbacks(zap); ui.postDelayed(zap, ZAP_DELAY_MS);
    }
    private void applyZap() {
        ui.removeCallbacks(zap);
        int target = pendingIndex; pendingIndex = -1;
        if (target < 0 || target == index || target >= queue.size() || !alive) return;
        if (prompt != null) prompt.dismiss();
        stopPlayer(); index = target; media = queue.get(index);
        try { positionKey = key(media); } catch (Exception e) { return; }
        position = getSharedPreferences("playback", 0).getLong(positionKey, 0); resume = true; askingResume = false;
        start(true);
    }

    // ---- Options panel ---------------------------------------------------------------------
    private void settings() {
        if (player == null) return;
        ArrayList<String> labels = new ArrayList<>(); ArrayList<Runnable> actions = new ArrayList<>();
        labels.add(getString(R.string.text_quality)); actions.add(() -> tracks(C.TRACK_TYPE_VIDEO));
        labels.add(getString(R.string.text_audio_language)); actions.add(() -> tracks(C.TRACK_TYPE_AUDIO));
        labels.add(getString(R.string.text_subtitles)); actions.add(() -> tracks(C.TRACK_TYPE_TEXT));
        labels.add(getString(R.string.player_picture) + "  ·  " + getString(resizeLabel(resizeIndex()))); actions.add(this::picture);
        if (seekable()) { labels.add(getString(R.string.player_speed) + "  ·  " + speedLabel(speed)); actions.add(this::speeds); }
        overlay.show();
        show(new TvDialog(this, getString(R.string.player_eyebrow), getString(R.string.text_playback_options), "")
            .choices(labels.toArray(new String[0]), n -> actions.get(n).run()).action(getString(R.string.text_back), null));
    }
    private int resizeIndex() { int mode = view.getResizeMode(); for (int i = 0; i < RESIZE_MODES.length; i++) if (RESIZE_MODES[i] == mode) return i; return 0; }
    private static int resizeLabel(int i) { return i == 1 ? R.string.player_picture_zoom : i == 2 ? R.string.player_picture_stretch : R.string.player_picture_fit; }
    private void picture() {
        String[] labels = new String[RESIZE_MODES.length]; int current = resizeIndex();
        for (int i = 0; i < labels.length; i++) labels[i] = (i == current ? "✓  " : "") + getString(resizeLabel(i));
        show(new TvDialog(this, getString(R.string.text_playback_options), getString(R.string.player_picture), "")
            .choices(labels, n -> { view.setResizeMode(RESIZE_MODES[n]); getSharedPreferences("player", 0).edit().putInt("resize", n).apply(); })
            .action(getString(R.string.text_back), this::settings));
    }
    private String speedLabel(float value) { return value == 1f ? getString(R.string.player_speed_normal) : String.format(java.util.Locale.US, "%sx", value % 1 == 0 ? String.valueOf((int)value) : String.valueOf(value)); }
    private void speeds() {
        String[] labels = new String[SPEEDS.length];
        for (int i = 0; i < SPEEDS.length; i++) labels[i] = (SPEEDS[i] == speed ? "✓  " : "") + speedLabel(SPEEDS[i]);
        show(new TvDialog(this, getString(R.string.text_playback_options), getString(R.string.player_speed), "")
            .choices(labels, n -> { speed = SPEEDS[n]; if (player != null) player.setPlaybackParameters(new PlaybackParameters(speed)); })
            .action(getString(R.string.text_back), this::settings));
    }
    private void tracks(int type) {
        if (player == null) return;
        ArrayList<String> labels = new ArrayList<>(); ArrayList<TrackSelectionOverride> choices = new ArrayList<>();
        labels.add(type == C.TRACK_TYPE_TEXT ? getString(R.string.text_subtitles_off) : getString(R.string.text_automatic)); choices.add(null);
        if (type == C.TRACK_TYPE_TEXT) { labels.add(getString(R.string.text_automatic_preferred_language)); choices.add(null); }
        for (Tracks.Group g : player.getCurrentTracks().getGroups()) if (g.getType() == type)
            for (int i = 0; i < g.length; i++) {
                if (!g.isTrackSupported(i)) continue;
                androidx.media3.common.Format f = g.getTrackFormat(i);
                String label = f.label != null ? f.label : f.language != null ? f.language : getString(R.string.text_option) + (choices.size());
                if (type == C.TRACK_TYPE_VIDEO) label = f.height > 0 ? f.height + "p" : label;
                if (type == C.TRACK_TYPE_VIDEO && f.bitrate > 0) label += String.format(java.util.Locale.US, " · %.1f Mbps", f.bitrate / 1_000_000d);
                if (g.isTrackSelected(i) && (type != C.TRACK_TYPE_VIDEO || !player.getTrackSelectionParameters().overrides.isEmpty())) label = "✓  " + label;
                labels.add(label); choices.add(new TrackSelectionOverride(g.getMediaTrackGroup(), Collections.singletonList(i)));
            }
        String title = type == C.TRACK_TYPE_VIDEO ? getString(R.string.text_available_source_quality) : type == C.TRACK_TYPE_AUDIO ? getString(R.string.text_audio) : getString(R.string.text_subtitles);
        show(new TvDialog(this, getString(R.string.text_playback_options), title, "")
            .choices(labels.toArray(new String[0]), n -> {
                if (player == null) return;
                TrackSelectionParameters.Builder parameters = player.getTrackSelectionParameters().buildUpon().clearOverridesOfType(type);
                parameters.setTrackTypeDisabled(type, type == C.TRACK_TYPE_TEXT && n == 0);
                if (choices.get(n) != null) parameters.addOverride(choices.get(n)); player.setTrackSelectionParameters(parameters.build());
            }).action(getString(R.string.text_back), this::settings));
    }

    // ---- Lifecycle -------------------------------------------------------------------------
    private void save() {
        if (player == null) return;
        // While the resume question is open the player sits at 0; keep the saved position untouched.
        if (askingResume) { resume = true; return; }
        position = player.isCurrentMediaItemLive() ? 0 : player.getPlaybackState() == Player.STATE_ENDED ? 0 : player.getCurrentPosition();
        resume = player.getPlayWhenReady(); getSharedPreferences("playback", 0).edit().putLong(positionKey, position).apply();
    }
    @Override protected void onSaveInstanceState(Bundle out) { save(); out.putLong("position", position); out.putBoolean("play", resume); super.onSaveInstanceState(out); }
    private void stopPlayer() {
        save(); ui.removeCallbacksAndMessages(null);
        if (player != null) {
            androidx.media3.exoplayer.DecoderCounters counters = player.getVideoDecoderCounters();
            if (counters != null) {
                counters.ensureUpdated(); final int rendered = counters.renderedOutputBufferCount, dropped = counters.droppedBufferCount;
                audit.execute(() -> { try { Store store = new Store(getApplicationContext());
                    store.audit("video_frames_rendered", rendered); store.audit("video_frames_dropped", dropped);
                } catch (Exception ignored) { } });
            }
            overlay.attach(null); view.setPlayer(null); player.release(); player = null;
        }
    }
    @Override protected void onStop() {
        if (prompt != null) prompt.dismiss(); pendingIndex = -1; stopPlayer(); overlay.hideNow();
        super.onStop();
    }
    @Override protected void onDestroy() { alive = false; if (overlay != null) overlay.release(); audit.shutdown(); super.onDestroy(); }
}
