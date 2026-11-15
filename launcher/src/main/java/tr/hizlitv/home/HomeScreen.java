package tr.hizlitv.home;

import tr.hizlitv.ui.R;
import android.content.Context;
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

/**
 * Quiet native dashboard: a small icon row for device tools, a greeting panel that
 * describes the focused item, a favorites shelf and the full app grid on one screen.
 */
final class HomeScreen extends LinearLayout {
    interface Actions { void language(); void action(HomeAction action); void open(AppCatalog.Entry app); void pin(AppCatalog.Entry app); }
    private static final int COLUMNS = 5;
    private final Actions actions;
    private final TextView headline, caption, clock, dateLabel, ramPercent, ramAvailable, feedback;
    private final LinearLayout rows;
    private final ScrollView scroll;
    private final FrameLayout ramTrack;
    private final View ramFill;
    private final Map<HomeAction, View> tools = new EnumMap<>(HomeAction.class);
    private final List<View> favoriteTiles = new ArrayList<>(), allTiles = new ArrayList<>();
    private int shownRevision = -1;
    /** Section-qualified so the same app in favorites and all apps keeps its own focus. */
    private String focusedKey = "";
    private final SimpleDateFormat time;
    private final SimpleDateFormat date;
    HomeScreen(Context c, Actions actions) {
        super(c); this.actions = actions;
        time = new SimpleDateFormat("HH:mm", tr.hizlitv.ui.Language.locale(c));
        date = new SimpleDateFormat("d MMMM EEEE", tr.hizlitv.ui.Language.locale(c)); setOrientation(VERTICAL); setClipChildren(false);
        setPadding(d(42), d(18), d(42), d(14)); setBackground(new AmbientBackground());

        LinearLayout header = TvUi.row(c);
        TextView brand = TvUi.text(c, "HızlıTV", 23, TvUi.TEXT, true); header.addView(brand, new LayoutParams(0, -2, 1));
        for (HomeAction action : HomeAction.values()) {
            View tool = toolButton(action); tools.put(action, tool);
            LayoutParams tp = new LayoutParams(d(36), d(36)); tp.rightMargin = d(10); header.addView(tool, tp);
        }
        Button language = TvUi.button(c, tr.hizlitv.ui.Language.code(c).toUpperCase(Locale.ROOT), actions::language);
        language.setTextSize(11); language.setContentDescription(c.getString(R.string.language));
        language.setOnFocusChangeListener((v, focused) -> { if (focused) focusedKey = ""; });
        LayoutParams languageParams = new LayoutParams(-2, d(32)); languageParams.leftMargin = d(4); languageParams.rightMargin = d(18); header.addView(language, languageParams);
        dateLabel = TvUi.text(c, "", 11, 0xffa6b7ca, false); dateLabel.setPadding(0, 0, d(14), 0); header.addView(dateLabel);
        clock = TvUi.text(c, "", 22, TvUi.TEXT, true); header.addView(clock); addView(header, new LayoutParams(-1, d(42)));

        // Greeting panel: describes whatever has focus; RAM is a quiet summary inside it.
        LinearLayout welcome = TvUi.row(c); welcome.setPadding(d(21), 0, d(18), 0);
        GradientDrawable glow = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0x70334d68, 0x302d3d52});
        glow.setCornerRadius(d(17)); glow.setStroke(d(1), 0x18ffffff); welcome.setBackground(glow);
        LinearLayout texts = new LinearLayout(c); texts.setOrientation(VERTICAL); texts.setGravity(Gravity.CENTER_VERTICAL);
        headline = TvUi.text(c, getContext().getString(R.string.text_enjoy_watching), 27, TvUi.TEXT, true); headline.setSingleLine(); headline.setEllipsize(TextUtils.TruncateAt.END);
        caption = TvUi.text(c, getContext().getString(R.string.text_your_channels_films_and_favorites_together), 12, 0xffb6c8db, false); caption.setSingleLine(); caption.setEllipsize(TextUtils.TruncateAt.END); caption.setPadding(0, d(5), 0, 0);
        texts.addView(headline); texts.addView(caption); welcome.addView(texts, new LayoutParams(0, -2, 1));
        LinearLayout memory = new LinearLayout(c); memory.setOrientation(VERTICAL); memory.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout memoryTop = TvUi.row(c); memoryTop.addView(TvUi.text(c, "RAM", 11, 0xffacbfd2, true), new LayoutParams(0, -2, 1));
        ramPercent = TvUi.text(c, "", 18, TvUi.TEXT, true); memoryTop.addView(ramPercent); memory.addView(memoryTop);
        ramAvailable = TvUi.text(c, getContext().getString(R.string.text_measuring), 11, 0xffb8cbdb, false); memory.addView(ramAvailable);
        ramTrack = new FrameLayout(c); ramTrack.setBackground(TvUi.shape(c, 0x24ffffff, 2, 0));
        ramFill = new View(c); ramFill.setBackground(TvUi.shape(c, 0xff96c7dc, 2, 0)); ramTrack.addView(ramFill, new FrameLayout.LayoutParams(1, -1));
        LayoutParams tp = new LayoutParams(-1, d(3)); tp.topMargin = d(6); memory.addView(ramTrack, tp);
        welcome.addView(memory, new LayoutParams(d(150), -2));
        LayoutParams wp = new LayoutParams(-1, d(84)); wp.topMargin = d(8); wp.bottomMargin = d(10); addView(welcome, wp);

        scroll = new ScrollView(c); scroll.setClipToPadding(false); scroll.setClipChildren(false); scroll.setVerticalScrollBarEnabled(false); scroll.setPadding(d(3), d(3), d(3), 0);
        rows = new LinearLayout(c); rows.setOrientation(VERTICAL); rows.setClipChildren(false); scroll.addView(rows); addView(scroll, new LayoutParams(-1, 0, 1));
        feedback = TvUi.text(c, "", 11, TvUi.ACCENT, false); feedback.setSingleLine(); feedback.setEllipsize(TextUtils.TruncateAt.END); feedback.setVisibility(GONE); addView(feedback);
        TextView hint = TvUi.text(c, getContext().getString(R.string.home_hint), 10, 0xff91a6bc, false); hint.setPadding(0, d(5), 0, 0); addView(hint);
    }
    private int d(float n) { return TvUi.dp(getContext(), n); }
    /** Round icon-only tool; its name appears in the greeting panel while focused. */
    private View toolButton(HomeAction action) {
        Context c = getContext(); String title = c.getString(action.title);
        ImageButton button = new ImageButton(c); button.setImageDrawable(new ActionIcon(action)); button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setPadding(d(9), d(9), d(9), d(9)); button.setFocusable(true); button.setContentDescription(title);
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused}, oval(0xffedf5fb));
        background.addState(new int[]{android.R.attr.state_pressed}, oval(0xffc5dded));
        background.addState(new int[]{}, oval(0x70303e50)); button.setBackground(background);
        button.setOnFocusChangeListener((v, focused) -> { if (focused) { focusedKey = ""; headline.setText(title); caption.setText(c.getString(R.string.text_press_ok_to_open)); } });
        button.setOnClickListener(v -> actions.action(action)); return button;
    }
    private static GradientDrawable oval(int color) { GradientDrawable d = new GradientDrawable(); d.setShape(GradientDrawable.OVAL); d.setColor(color); return d; }
    /** Hides tools whose target is not installed (the optional AirPlay receiver). */
    void tool(HomeAction action, boolean available) {
        View tool = tools.get(action); if (tool == null) return;
        if (!available && tool.hasFocus()) focusFirst();
        tool.setVisibility(available ? VISIBLE : GONE);
    }
    private final Runnable clockTick = new Runnable() {
        public void run() { Date now = new Date(); clock.setText(time.format(now)); dateLabel.setText(date.format(now)); postDelayed(this, 60000 - System.currentTimeMillis() % 60000); }
    };
    void startClock() { stopClock(); clockTick.run(); }
    void stopClock() { removeCallbacks(clockTick); }
    @Override protected void onDetachedFromWindow() { stopClock(); super.onDetachedFromWindow(); }
    void memory(int used, long available, String result) {
        ramPercent.setText(getContext().getString(R.string.ram_percent, used)); ramAvailable.setText(getContext().getString(R.string.ram_available, available));
        ramTrack.post(() -> { int width = Math.max(d(2), ramTrack.getWidth() * used / 100); android.view.ViewGroup.LayoutParams params = ramFill.getLayoutParams(); if (params.width != width) { params.width = width; ramFill.setLayoutParams(params); } });
        feedback.setText(result); feedback.setVisibility(result.isEmpty() ? GONE : VISIBLE);
    }
    /** Builds the favorites shelf and the all-apps grid; skipped when nothing changed. */
    void show(AppCatalog catalog) {
        if (shownRevision == catalog.revision()) return;
        // A focused tool/language button keeps focus; otherwise focus returns to the same tile.
        boolean keepHeaderFocus = focusedKey.isEmpty() && findFocus() != null;
        shownRevision = catalog.revision(); rows.removeAllViews(); favoriteTiles.clear(); allTiles.clear();
        View selected = null;
        rows.addView(heading(R.string.text_your_favorites));
        List<AppCatalog.Entry> favorites = catalog.entries(false);
        if (favorites.isEmpty()) {
            TextView empty = TvUi.text(getContext(), getContext().getString(R.string.text_choose_a_card_in_all_apps_and_hold_ok_to_pin_it), 14, TvUi.MUTED, false);
            empty.setPadding(d(8), d(4), d(8), d(14)); rows.addView(empty);
        } else selected = grid(favorites, catalog, "fav:", favoriteTiles, selected);
        rows.addView(heading(R.string.text_all_apps));
        selected = grid(catalog.entries(true), catalog, "all:", allTiles, selected);
        View focus = selected != null ? selected : !favoriteTiles.isEmpty() ? favoriteTiles.get(0) : allTiles.isEmpty() ? null : allTiles.get(0);
        if (focus != null && !keepHeaderFocus) focus.requestFocus();
    }
    private TextView heading(int text) {
        TextView heading = TvUi.text(getContext(), getContext().getString(text), 14, 0xffdce5ef, true); heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setLayoutParams(new LayoutParams(-1, d(32))); return heading;
    }
    private View grid(List<AppCatalog.Entry> apps, AppCatalog catalog, String section, List<View> tiles, View selected) {
        for (int start = 0; start < apps.size(); start += COLUMNS) {
            LinearLayout row = new LinearLayout(getContext()); row.setClipChildren(false); LayoutParams rp = new LayoutParams(-1, d(123)); rp.bottomMargin = d(8); rows.addView(row, rp);
            for (int column = 0; column < COLUMNS; column++) {
                int index = start + column; LayoutParams p = new LayoutParams(0, -1, 1); if (column < COLUMNS - 1) p.rightMargin = d(19);
                if (index >= apps.size()) { row.addView(new View(getContext()), p); continue; }
                AppCatalog.Entry app = apps.get(index); String key = section + app.pkg;
                View tile = tile(app, catalog.pinned(app), key); row.addView(tile, p); tiles.add(tile);
                if (key.equals(focusedKey)) selected = tile;
            }
        }
        return selected;
    }
    /** Menu jumps between the favorites shelf and the full grid. */
    void jump() {
        View focused = findFocus();
        boolean inAll = focused != null && allTiles.contains(focused);
        List<View> target = inAll || allTiles.isEmpty() ? favoriteTiles : allTiles;
        if (target.isEmpty()) target = allTiles;
        if (!target.isEmpty()) target.get(0).requestFocus();
    }
    /** Back from the full grid returns to the favorites shelf first. */
    boolean back() {
        View focused = findFocus();
        if (focused == null || !allTiles.contains(focused) || favoriteTiles.isEmpty()) return false;
        favoriteTiles.get(0).requestFocus(); scroll.smoothScrollTo(0, 0); return true;
    }
    private void focusFirst() {
        if (!favoriteTiles.isEmpty()) favoriteTiles.get(0).requestFocus(); else if (!allTiles.isEmpty()) allTiles.get(0).requestFocus();
    }
    private View tile(AppCatalog.Entry app, boolean pinned, String key) {
        Context c = getContext(); LinearLayout tile = new LinearLayout(c); tile.setOrientation(VERTICAL); tile.setClipChildren(false); tile.setFocusable(true); tile.setClickable(true); tile.setContentDescription(app.name + (pinned ? getContext().getString(R.string.text_favorite) : ""));
        FrameLayout frame = new FrameLayout(c);
        StateListDrawable focusFrame = new StateListDrawable();
        focusFrame.addState(new int[]{android.R.attr.state_activated}, TvUi.shape(c, 0xffe7f3fc, 15, 0));
        focusFrame.addState(new int[]{}, TvUi.shape(c, 0x335f7890, 15, 0)); frame.setBackground(focusFrame); frame.setPadding(d(3), d(3), d(3), d(3)); tile.addView(frame, new LayoutParams(-1, d(89)));
        FrameLayout art = new FrameLayout(c); art.setBackground(TvUi.shape(c, 0xff142131, 12, 0)); art.setClipToOutline(true); frame.addView(art, new FrameLayout.LayoutParams(-1, -1));
        boolean player = app.pkg.equals(AppTargets.PLAYER);
        if (player) {
            GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xff3e648c, 0xff283b65}); gradient.setCornerRadius(d(12)); art.setBackground(gradient);
            LinearLayout branding = new LinearLayout(c); branding.setOrientation(VERTICAL); branding.setGravity(Gravity.CENTER_VERTICAL); branding.setPadding(d(14), 0, 0, 0);
            branding.addView(TvUi.text(c, "HızlıTV", 17, TvUi.TEXT, true)); TextView label = TvUi.text(c, "PLAYER", 9, 0xffbbd5ed, true); label.setLetterSpacing(.12f); label.setPadding(0, d(4), 0, 0); branding.addView(label); art.addView(branding, new FrameLayout.LayoutParams(-1, -1));
            TextView mark = TvUi.text(c, "▶", 22, 0xffd9f0fb, false); mark.setGravity(Gravity.CENTER); mark.setBackground(TvUi.shape(c, 0x304e97bd, 24, 0)); FrameLayout.LayoutParams markParams = new FrameLayout.LayoutParams(d(40), d(40), Gravity.END | Gravity.CENTER_VERTICAL); markParams.setMarginEnd(d(12)); art.addView(mark, markParams);
        } else if (app.artwork != null) {
            ImageView icon = new ImageView(c); icon.setImageDrawable(app.artwork); icon.setScaleType(app.banner ? ImageView.ScaleType.FIT_XY : ImageView.ScaleType.FIT_CENTER); int padding = app.banner ? 0 : d(14); icon.setPadding(padding, padding, padding, padding); art.addView(icon, new FrameLayout.LayoutParams(-1, -1));
        }
        TextView label = TvUi.text(c, player ? "Player" : app.name, 12, 0xffa9bdd1, false); label.setSingleLine(); label.setEllipsize(TextUtils.TruncateAt.END); label.setGravity(Gravity.CENTER); label.setPadding(d(2), d(8), d(2), 0); tile.addView(label, new LayoutParams(-1, -2));
        tile.setOnFocusChangeListener((v, focused) -> {
            frame.setActivated(focused); frame.setScaleX(focused ? 1.025f : 1); frame.setScaleY(focused ? 1.025f : 1); label.setTextColor(focused ? TvUi.TEXT : 0xffa9bdd1);
            if (focused) {
                focusedKey = key; headline.setText(player ? getContext().getString(R.string.text_enjoy_watching) : app.name);
                caption.setText(player ? getContext().getString(R.string.text_your_channels_films_and_shared_links) : pinned ? getContext().getString(R.string.text_in_your_favorites_press_ok_to_open) : getContext().getString(R.string.text_press_ok_to_open));
            }
        });
        tile.setOnClickListener(v -> actions.open(app)); tile.setOnLongClickListener(v -> { actions.pin(app); return true; }); return tile;
    }
}
