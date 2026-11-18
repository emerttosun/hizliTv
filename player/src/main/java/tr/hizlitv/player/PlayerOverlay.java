package tr.hizlitv.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import tr.hizlitv.ui.R;
import tr.hizlitv.ui.TvUi;

/**
 * Remote-first playback chrome: title/clock on top, a large progress bar below and a short key hint.
 * It only draws; the Activity owns keys and the player. Updates run only while it is visible.
 */
final class PlayerOverlay extends FrameLayout {
    private static final long HIDE_AFTER_MS = 5000, TICK_MS = 500;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final LinearLayout top, bottom;
    private final TextView title, detail, live, clock, position, duration, hint, status;
    private final View state;
    private final Progress progress;
    private final SimpleDateFormat time;
    private Player player;
    private boolean shown;
    private final Runnable hide = this::hideNow;
    private final Runnable tick = new Runnable() { public void run() { refresh(); if (shown) ui.postDelayed(this, TICK_MS); } };
    private final Runnable clearStatus = this::clearMessage;

    PlayerOverlay(Context c) {
        super(c); time = new SimpleDateFormat("HH:mm", tr.hizlitv.ui.Language.locale(c));
        top = new LinearLayout(c); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.TOP);
        top.setPadding(d(42), d(24), d(42), d(40)); top.setBackground(scrim(GradientDrawable.Orientation.TOP_BOTTOM));
        LinearLayout names = new LinearLayout(c); names.setOrientation(LinearLayout.VERTICAL);
        title = TvUi.text(c, "", 24, TvUi.TEXT, true); title.setSingleLine(); title.setEllipsize(TextUtils.TruncateAt.END); names.addView(title);
        detail = TvUi.text(c, "", 13, 0xffc2d1e0, false); detail.setSingleLine(); detail.setEllipsize(TextUtils.TruncateAt.END); detail.setPadding(0, d(4), 0, 0); names.addView(detail);
        top.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        live = TvUi.text(c, c.getString(R.string.player_live), 12, 0xffffffff, true); live.setLetterSpacing(.08f);
        live.setPadding(d(10), d(3), d(10), d(3)); live.setBackground(TvUi.shape(c, 0xffc8323c, 6, 0)); live.setVisibility(GONE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.rightMargin = d(16); lp.topMargin = d(6); top.addView(live, lp);
        clock = TvUi.text(c, "", 22, TvUi.TEXT, true); top.addView(clock);
        addView(top, new LayoutParams(-1, -2, Gravity.TOP));

        bottom = new LinearLayout(c); bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setPadding(d(42), d(46), d(42), d(22)); bottom.setBackground(scrim(GradientDrawable.Orientation.BOTTOM_TOP));
        LinearLayout bar = TvUi.row(c);
        position = TvUi.text(c, "", 14, TvUi.TEXT, true); bar.addView(position, new LinearLayout.LayoutParams(d(76), -2));
        progress = new Progress(c); bar.addView(progress, new LinearLayout.LayoutParams(0, d(20), 1));
        duration = TvUi.text(c, "", 14, 0xffc2d1e0, false); duration.setGravity(Gravity.END); bar.addView(duration, new LinearLayout.LayoutParams(d(84), -2));
        bottom.addView(bar);
        hint = TvUi.text(c, "", 11, 0xffa9bacb, false); hint.setPadding(0, d(10), 0, 0); bottom.addView(hint);
        addView(bottom, new LayoutParams(-1, -2, Gravity.BOTTOM));

        // Centre: a paused marker and short-lived status (seek step, reconnecting, next channel).
        state = new PauseMark(c); state.setBackground(oval(0x99101820)); state.setVisibility(GONE); addView(state, new LayoutParams(d(84), d(84), Gravity.CENTER));
        status = TvUi.text(c, "", 16, TvUi.TEXT, true); status.setGravity(Gravity.CENTER); status.setPadding(d(18), d(8), d(18), d(8));
        status.setBackground(TvUi.shape(c, 0xb3101820, 14, 0)); status.setVisibility(GONE);
        LayoutParams sp = new LayoutParams(-2, -2, Gravity.CENTER); sp.topMargin = d(104); addView(status, sp);
        setVisibility(GONE);
    }
    private int d(float n) { return TvUi.dp(getContext(), n); }
    private static GradientDrawable scrim(GradientDrawable.Orientation from) { return new GradientDrawable(from, new int[]{0xd9000000, 0x00000000}); }
    private static GradientDrawable oval(int color) { GradientDrawable g = new GradientDrawable(); g.setShape(GradientDrawable.OVAL); g.setColor(color); return g; }

    void attach(Player next) { player = next; refresh(); }
    void describe(String name, String info, String keys) { title.setText(name); detail.setText(info); detail.setVisibility(info.isEmpty() ? GONE : VISIBLE); hint.setText(keys); }
    boolean shown() { return shown; }
    /** Shows the chrome; it hides by itself after a few seconds unless playback is paused. */
    void show() {
        shown = true; setVisibility(VISIBLE); ui.removeCallbacks(tick); tick.run(); scheduleHide();
    }
    void hideNow() { shown = false; ui.removeCallbacks(hide); ui.removeCallbacks(tick); setVisibility(GONE); }
    void scheduleHide() { ui.removeCallbacks(hide); if (player != null && player.isPlaying()) ui.postDelayed(hide, HIDE_AFTER_MS); }
    /** Brief centred message, e.g. "+30 s" or "Reconnecting…". */
    void message(String text, long millis) {
        status.setText(text); status.setVisibility(VISIBLE); ui.removeCallbacks(clearStatus);
        if (millis > 0) ui.postDelayed(clearStatus, millis);
    }
    void clearMessage() { ui.removeCallbacks(clearStatus); status.setVisibility(GONE); }
    void release() { hideNow(); ui.removeCallbacksAndMessages(null); player = null; }

    private void refresh() {
        clock.setText(time.format(new Date()));
        if (player == null) return;
        long dur = player.getDuration(), pos = Math.max(0, player.getCurrentPosition());
        // The badge needs Media3 to report a live timeline; an unknown duration (still loading, failed) only hides the bar.
        boolean isLive = player.isCurrentMediaItemLive(), timeless = isLive || dur == C.TIME_UNSET;
        live.setVisibility(isLive ? VISIBLE : GONE);
        progress.setVisibility(timeless ? INVISIBLE : VISIBLE); position.setVisibility(timeless ? INVISIBLE : VISIBLE); duration.setVisibility(timeless ? INVISIBLE : VISIBLE);
        if (!timeless) {
            position.setText(format(pos)); duration.setText("−" + format(Math.max(0, dur - pos)));
            progress.set(pos, player.getBufferedPosition(), dur);
        }
        boolean paused = !player.getPlayWhenReady() && player.getPlaybackState() != Player.STATE_ENDED;
        state.setVisibility(paused ? VISIBLE : GONE);
    }
    static String format(long ms) {
        long s = Math.max(0, ms) / 1000, h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, sec) : String.format(Locale.US, "%d:%02d", m, sec);
    }
    @Override protected void onDetachedFromWindow() { ui.removeCallbacksAndMessages(null); super.onDetachedFromWindow(); }

    /** Two drawn bars, so the paused marker never depends on a font glyph. */
    private static final class PauseMark extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        PauseMark(Context c) { super(c); paint.setColor(0xffffffff); }
        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight(), bar = w * .11f, gap = w * .09f, top = h * .32f, bottom = h * .68f, r = bar / 3;
            rect.set(w / 2 - gap / 2 - bar, top, w / 2 - gap / 2, bottom); canvas.drawRoundRect(rect, r, r, paint);
            rect.set(w / 2 + gap / 2, top, w / 2 + gap / 2 + bar, bottom); canvas.drawRoundRect(rect, r, r, paint);
        }
    }
    /** Thin track with buffered and played segments and a round head; no per-frame animation. */
    private static final class Progress extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private long pos, buffered, dur;
        Progress(Context c) { super(c); }
        void set(long p, long b, long d) { if (p == pos && b == buffered && d == dur) return; pos = p; buffered = b; dur = d; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            float h = getHeight(), w = getWidth(), line = h * .25f, cy = h / 2, r = line / 2, margin = h / 2;
            float left = margin, right = w - margin, span = Math.max(1, right - left);
            float played = dur > 0 ? left + span * Math.min(1f, pos / (float)dur) : left;
            float loaded = dur > 0 ? left + span * Math.min(1f, buffered / (float)dur) : left;
            paint.setColor(0x40ffffff); rect.set(left, cy - r, right, cy + r); canvas.drawRoundRect(rect, r, r, paint);
            paint.setColor(0x80ffffff); rect.set(left, cy - r, Math.max(left, loaded), cy + r); canvas.drawRoundRect(rect, r, r, paint);
            paint.setColor(0xffb9d4ff); rect.set(left, cy - r, Math.max(left, played), cy + r); canvas.drawRoundRect(rect, r, r, paint);
            paint.setColor(0xffffffff); canvas.drawCircle(played, cy, h * .38f, paint);
        }
    }
}
