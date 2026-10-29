package tr.hizlitv.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import tr.hizlitv.ui.AmbientBackground;
import tr.hizlitv.ui.TvUi;

/** Quiet native dashboard with individually visible tools and real Android DPAD focus. */
final class HomeScreen extends LinearLayout {
    interface Actions { void action(HomeAction action); void section(boolean all); void open(AppCatalog.Entry app); void pin(AppCatalog.Entry app); }
    private final Actions actions;
    private final TextView headline, caption, clock, dateLabel, ramPercent, ramAvailable, heading, feedback;
    private final LinearLayout rows;
    private final FrameLayout ramTrack;
    private final View ramFill;
    private final Button appsButton;
    private int shownRevision = -1;
    private boolean shownAll;
    private String focusedPackage = "";
    private final SimpleDateFormat time = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat date = new SimpleDateFormat("d MMMM EEEE", new Locale("tr", "TR"));
    HomeScreen(Context c, Actions actions) {
        super(c); this.actions = actions; setOrientation(VERTICAL); setClipChildren(false);
        setPadding(d(42), d(18), d(42), d(14)); setBackground(new AmbientBackground());
        LinearLayout header = TvUi.row(c);
        TextView brand = TvUi.text(c, "HızlıTV", 23, TvUi.TEXT, true); header.addView(brand, new LayoutParams(0, -2, 1));
        dateLabel = TvUi.text(c, "", 11, 0xffa6b7ca, false); dateLabel.setPadding(0, 0, d(18), 0); header.addView(dateLabel);
        clock = TvUi.text(c, "", 22, TvUi.TEXT, true); header.addView(clock); addView(header, new LayoutParams(-1, d(42)));

        LinearLayout hero = TvUi.row(c); hero.setPadding(0, d(7), 0, d(3));
        LinearLayout welcome = new LinearLayout(c); welcome.setOrientation(VERTICAL); welcome.setGravity(Gravity.CENTER_VERTICAL); welcome.setPadding(d(21), 0, d(20), 0);
        GradientDrawable glow = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0x70334d68, 0x302d3d52});
        glow.setCornerRadius(d(17)); glow.setStroke(d(1), 0x18ffffff); welcome.setBackground(glow);
        headline = TvUi.text(c, "İyi seyirler.", 27, TvUi.TEXT, true); headline.setSingleLine(); headline.setEllipsize(TextUtils.TruncateAt.END);
        caption = TvUi.text(c, "Kanalların, filmlerin ve favorilerin burada.", 12, 0xffb6c8db, false); caption.setSingleLine(); caption.setEllipsize(TextUtils.TruncateAt.END); caption.setPadding(0, d(5), 0, 0);
        welcome.addView(headline); welcome.addView(caption); LayoutParams wp = new LayoutParams(0, -1, 1); wp.rightMargin = d(16); hero.addView(welcome, wp);
        LinearLayout memory = new LinearLayout(c); memory.setOrientation(VERTICAL); memory.setGravity(Gravity.CENTER_VERTICAL); memory.setPadding(d(16), d(8), d(16), d(8)); memory.setBackground(TvUi.shape(c, 0x65324457, 17, 0x16ffffff));
        LinearLayout memoryTop = TvUi.row(c); memoryTop.addView(TvUi.text(c, "RAM", 11, 0xffacbfd2, true), new LayoutParams(0, -2, 1));
        ramPercent = TvUi.text(c, "", 22, TvUi.TEXT, true); memoryTop.addView(ramPercent); memory.addView(memoryTop);
        ramAvailable = TvUi.text(c, "Ölçülüyor…", 11, 0xffb8cbdb, false); memory.addView(ramAvailable);
        ramTrack = new FrameLayout(c); ramTrack.setBackground(TvUi.shape(c, 0x24ffffff, 2, 0));
        ramFill = new View(c); ramFill.setBackground(TvUi.shape(c, 0xff96c7dc, 2, 0)); ramTrack.addView(ramFill, new FrameLayout.LayoutParams(1, -1));
        LayoutParams tp = new LayoutParams(-1, d(3)); tp.topMargin = d(7); memory.addView(ramTrack, tp);
        memory.setContentDescription("Bellekteki uygulamaları aç"); memory.setOnClickListener(v -> actions.action(HomeAction.PROCESSES));
        hero.addView(memory, new LayoutParams(d(202), -1)); addView(hero, new LayoutParams(-1, d(88)));

        LinearLayout tools = TvUi.row(c); Button apps = null;
        for (HomeAction action : HomeAction.values()) {
            Button button = actionButton(action); if (action == HomeAction.APPS) apps = button;
            LayoutParams bp = new LayoutParams(0, -1, action.weight); if (action != HomeAction.STOCK_HOME) bp.rightMargin = d(7); tools.addView(button, bp);
        }
        appsButton = apps; LayoutParams toolsParams = new LayoutParams(-1, d(40)); toolsParams.topMargin = d(9); toolsParams.bottomMargin = d(9); addView(tools, toolsParams);
        heading = TvUi.text(c, "Favorilerin", 14, 0xffdce5ef, true); heading.setGravity(Gravity.CENTER_VERTICAL); addView(heading, new LayoutParams(-1, d(32)));
        ScrollView scroll = new ScrollView(c); scroll.setClipToPadding(false); scroll.setClipChildren(false); scroll.setVerticalScrollBarEnabled(false); scroll.setPadding(d(3), d(3), d(3), 0);
        rows = new LinearLayout(c); rows.setOrientation(VERTICAL); rows.setClipChildren(false); scroll.addView(rows); addView(scroll, new LayoutParams(-1, 0, 1));
        feedback = TvUi.text(c, "", 11, TvUi.ACCENT, false); feedback.setSingleLine(); feedback.setEllipsize(TextUtils.TruncateAt.END); feedback.setVisibility(GONE); addView(feedback);
        TextView hint = TvUi.text(c, "OK  Aç     ·     Uzun OK  Favoriye ekle / çıkar     ·     Menü  Tüm uygulamalar", 10, 0xff91a6bc, false); hint.setPadding(0, d(5), 0, 0); addView(hint);
    }
    private int d(float n) { return TvUi.dp(getContext(), n); }
    private Button actionButton(HomeAction action) {
        Button button = TvUi.button(getContext(), action.title, () -> actions.action(action));
        button.setTextSize(11); button.setSingleLine(); button.setEllipsize(TextUtils.TruncateAt.END); button.setPadding(d(9), 0, d(9), 0);
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused}, TvUi.shape(getContext(), 0xffedf5fb, 12, 0));
        background.addState(new int[]{android.R.attr.state_pressed}, TvUi.shape(getContext(), 0xffc5dded, 12, 0));
        background.addState(new int[]{}, TvUi.shape(getContext(), action == HomeAction.PLAYER ? 0x90486784 : 0x70303e50, 12, 0x20ffffff)); button.setBackground(background);
        ActionIcon icon = new ActionIcon(action); icon.setBounds(0, 0, d(14), d(14)); button.setCompoundDrawables(icon, null, null, null); button.setCompoundDrawablePadding(d(5));
        button.setContentDescription(action.title); return button;
    }
    private final Runnable clockTick = new Runnable() {
        public void run() { Date now = new Date(); clock.setText(time.format(now)); dateLabel.setText(date.format(now)); postDelayed(this, 60000 - System.currentTimeMillis() % 60000); }
    };
    void startClock() { stopClock(); clockTick.run(); }
    void stopClock() { removeCallbacks(clockTick); }
    @Override protected void onDetachedFromWindow() { stopClock(); super.onDetachedFromWindow(); }
    void memory(int used, long available, String result) {
        ramPercent.setText("%" + used); ramAvailable.setText(available + " MB kullanılabilir");
        ramTrack.post(() -> { int width = Math.max(d(2), ramTrack.getWidth() * used / 100); android.view.ViewGroup.LayoutParams params = ramFill.getLayoutParams(); if (params.width != width) { params.width = width; ramFill.setLayoutParams(params); } });
        feedback.setText(result); feedback.setVisibility(result.isEmpty() ? GONE : VISIBLE);
    }
    void show(AppCatalog catalog, boolean all) {
        if (shownRevision == catalog.revision() && shownAll == all) return;
        shownRevision = catalog.revision(); shownAll = all;
        heading.setText(all ? "Tüm uygulamalar" : "Favorilerin"); appsButton.setText(all ? "Favoriler" : HomeAction.APPS.title); rows.removeAllViews();
        List<AppCatalog.Entry> visible = catalog.entries(all); View selected = null, first = null;
        if (visible.isEmpty()) {
            TextView empty = TvUi.text(getContext(), "Uygulamalar’dan bir kart seçip OK’ye uzun basarak favori ekleyebilirsin.", 16, TvUi.MUTED, false); empty.setPadding(d(8), d(20), d(8), 0); rows.addView(empty); appsButton.requestFocus(); return;
        }
        for (int start = 0; start < visible.size(); start += 5) {
            LinearLayout row = new LinearLayout(getContext()); row.setClipChildren(false); LayoutParams rp = new LayoutParams(-1, d(123)); rp.bottomMargin = d(8); rows.addView(row, rp);
            for (int column = 0; column < 5; column++) {
                int index = start + column; LayoutParams p = new LayoutParams(0, -1, 1); if (column < 4) p.rightMargin = d(19);
                if (index >= visible.size()) { row.addView(new View(getContext()), p); continue; }
                AppCatalog.Entry app = visible.get(index); View tile = tile(app, catalog.pinned(app), all); row.addView(tile, p); if (first == null) first = tile; if (app.pkg.equals(focusedPackage)) selected = tile;
            }
        }
        View focus = selected == null ? first : selected; if (focus != null) focus.requestFocus();
    }
    private View tile(AppCatalog.Entry app, boolean pinned, boolean all) {
        Context c = getContext(); LinearLayout tile = new LinearLayout(c); tile.setOrientation(VERTICAL); tile.setClipChildren(false); tile.setFocusable(true); tile.setClickable(true); tile.setContentDescription(app.name + (pinned ? ", favori" : ""));
        FrameLayout frame = new FrameLayout(c);
        StateListDrawable focusFrame = new StateListDrawable();
        focusFrame.addState(new int[]{android.R.attr.state_activated}, TvUi.shape(c, 0xffe7f3fc, 15, 0));
        focusFrame.addState(new int[]{}, TvUi.shape(c, 0x335f7890, 15, 0)); frame.setBackground(focusFrame); frame.setPadding(d(3), d(3), d(3), d(3)); tile.addView(frame, new LayoutParams(-1, d(89)));
        FrameLayout art = new FrameLayout(c); art.setBackground(TvUi.shape(c, 0xff142131, 12, 0)); art.setClipToOutline(true); frame.addView(art, new FrameLayout.LayoutParams(-1, -1));
        if (app.pkg.equals(AppTargets.PLAYER)) {
            GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xff3e648c, 0xff283b65}); gradient.setCornerRadius(d(12)); art.setBackground(gradient);
            LinearLayout branding = new LinearLayout(c); branding.setOrientation(VERTICAL); branding.setGravity(Gravity.CENTER_VERTICAL); branding.setPadding(d(14), 0, 0, 0);
            branding.addView(TvUi.text(c, "HızlıTV", 17, TvUi.TEXT, true)); TextView player = TvUi.text(c, "PLAYER", 9, 0xffbbd5ed, true); player.setLetterSpacing(.12f); player.setPadding(0, d(4), 0, 0); branding.addView(player); art.addView(branding, new FrameLayout.LayoutParams(-1, -1));
            TextView mark = TvUi.text(c, "▶", 22, 0xffd9f0fb, false); mark.setGravity(Gravity.CENTER); mark.setBackground(TvUi.shape(c, 0x304e97bd, 24, 0)); FrameLayout.LayoutParams markParams = new FrameLayout.LayoutParams(d(40), d(40), Gravity.END | Gravity.CENTER_VERTICAL); markParams.setMarginEnd(d(12)); art.addView(mark, markParams);
        } else if (app.artwork != null) {
            ImageView icon = new ImageView(c); icon.setImageDrawable(app.artwork); icon.setScaleType(app.banner ? ImageView.ScaleType.FIT_XY : ImageView.ScaleType.FIT_CENTER); int padding = app.banner ? 0 : d(14); icon.setPadding(padding, padding, padding, padding); art.addView(icon, new FrameLayout.LayoutParams(-1, -1));
        }
        TextView label = TvUi.text(c, app.pkg.equals(AppTargets.PLAYER) ? "Player" : app.name, 12, 0xffa9bdd1, false); label.setSingleLine(); label.setEllipsize(TextUtils.TruncateAt.END); label.setGravity(Gravity.CENTER); label.setPadding(d(2), d(8), d(2), 0); tile.addView(label, new LayoutParams(-1, -2));
        tile.setOnFocusChangeListener((v, focused) -> {
            frame.setActivated(focused); frame.setScaleX(focused ? 1.025f : 1); frame.setScaleY(focused ? 1.025f : 1); label.setTextColor(focused ? TvUi.TEXT : 0xffa9bdd1);
            if (focused) { focusedPackage = app.pkg; headline.setText(app.pkg.equals(AppTargets.PLAYER) ? "İyi seyirler." : app.name); caption.setText(app.pkg.equals(AppTargets.PLAYER) ? "Kanalların, filmlerin ve telefondan gönderdiklerin." : all && pinned ? "Favorilerinde  ·  Açmak için OK’ye bas." : "Açmak için OK’ye bas."); }
        });
        tile.setOnClickListener(v -> actions.open(app)); tile.setOnLongClickListener(v -> { actions.pin(app); return true; }); return tile;
    }
}
