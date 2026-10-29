package tr.hizlitv.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared native TV primitives. No network assets, background loop or animation. */
public final class TvUi {
    public static final int BACKGROUND = 0xff101114, SURFACE = 0xff202227,
        RAISED = 0xff2c2e34, TEXT = 0xfff4f4f6, MUTED = 0xffa8a9b1, ACCENT = 0xffb9d4ff;
    private TvUi() { }
    public static int dp(Context c, float value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
    public static void prepare(Activity activity) {
        activity.getWindow().getDecorView().setSystemUiVisibility(5894);
        activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }
    public static GradientDrawable shape(Context c, int color, float radius, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(c, radius));
        if (stroke != 0) d.setStroke(dp(c, 2), stroke);
        return d;
    }
    public static GradientDrawable backdrop() {
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xff191d27, BACKGROUND, 0xff15131b});
    }
    public static TextView text(Context c, String value, int size, int color, boolean medium) {
        TextView t = new TextView(c); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setFontFeatureSettings("kern");
        t.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return t;
    }
    public static Button button(Context c, String label, Runnable action) {
        Button b = new Button(c); b.setText(label); b.setTextSize(13); b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setMinWidth(0); b.setMinimumWidth(0); b.setMinHeight(0); b.setMinimumHeight(0);
        b.setStateListAnimator(null); b.setElevation(0); b.setTranslationZ(0); b.setPadding(dp(c, 20), 0, dp(c, 20), 0);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, shape(c, TEXT, 22, 0));
        bg.addState(new int[]{android.R.attr.state_pressed}, shape(c, 0xffc9d8ef, 22, 0));
        bg.addState(new int[]{}, shape(c, SURFACE, 22, 0));
        b.setBackground(bg);
        b.setTextColor(new ColorStateList(new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}},
            new int[]{BACKGROUND, TEXT}));
        b.setOnClickListener(v -> action.run()); return b;
    }
    public static LinearLayout screen(Context c) {
        LinearLayout layout = new LinearLayout(c); layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(c, 42), dp(c, 22), dp(c, 42), dp(c, 18));
        layout.setBackground(backdrop()); return layout;
    }
    public static LinearLayout row(Context c) {
        LinearLayout row = new LinearLayout(c); row.setGravity(Gravity.CENTER_VERTICAL); return row;
    }
    public static void addButton(LinearLayout row, Button button) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(row.getContext(), 42));
        p.setMargins(0, 0, dp(row.getContext(), 10), 0); row.addView(button, p);
    }
    public static TextView heading(Context c, String title, String eyebrow) {
        TextView t = text(c, eyebrow + "\n" + title, 28, TEXT, true); return t;
    }
}
