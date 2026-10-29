package tr.hizlitv.player;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import tr.hizlitv.media.LibraryIndex;
import tr.hizlitv.ui.AmbientBackground;
import android.os.Handler;
import android.os.Looper;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import tr.hizlitv.media.MediaEntry;
import tr.hizlitv.ui.TvUi;

/** The library presentation owns focus and filtering; it does not fetch or store media. */
final class LibraryScreen extends LinearLayout {
    interface Actions { void phone(); void input(); void undo(); void home(); void groups(); void open(MediaEntry entry); void edit(MediaEntry entry); }
    private LibraryIndex index = new LibraryIndex(java.util.Collections.emptyList());
    private final List<View> edits = new ArrayList<>();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable searchChanged = this::render;
    private List<MediaEntry> visible = new ArrayList<>();
    private final TextView status, emptyTitle, emptyHint;
    private final EditText search;
    private final LinearLayout empty;
    private final ListView list;
    private final BaseAdapter adapter;
    private final Button all, favoritesButton, groups;
    private boolean favorites;
    private String group = "";
    LibraryScreen(Context c, Actions actions) {
        super(c); setOrientation(VERTICAL); setBackground(new AmbientBackground());
        setPadding(d(42), d(20), d(42), d(16));
        LinearLayout header = TvUi.row(c);
        LinearLayout brand = TvUi.row(c);
        brand.addView(TvUi.text(c, "HızlıTV", 23, TvUi.TEXT, true));
        TextView player = TvUi.text(c, "  /  Player", 16, 0xffa6bed6, false); brand.addView(player);
        header.addView(brand, new LayoutParams(0, -2, 1));
        TvUi.addButton(header, TvUi.button(c, "← HızlıTV", actions::home)); addView(header, new LayoutParams(-1, d(46)));
        LinearLayout intro = TvUi.row(c); intro.setPadding(0, d(13), 0, d(10));
        LinearLayout titles = new LinearLayout(c); titles.setOrientation(VERTICAL);
        titles.addView(TvUi.text(c, "Kütüphanen", 30, TvUi.TEXT, true));
        status = TvUi.text(c, "Filmler ve canlı kanallar, tek yerde.", 12, 0xffafc2d5, false);
        status.setSingleLine(); status.setEllipsize(TextUtils.TruncateAt.END); status.setPadding(0, d(4), 0, 0); titles.addView(status);
        intro.addView(titles, new LayoutParams(0, -2, 1));
        Button send = TvUi.button(c, "Telefondan ekle", actions::phone); edits.add(send); TvUi.addButton(intro, send);
        Button input = TvUi.button(c, "Bağlantı / liste", actions::input); edits.add(input); TvUi.addButton(intro, input);
        Button undo = TvUi.button(c, "Geri al", actions::undo); edits.add(undo); TvUi.addButton(intro, undo); addView(intro);
        LinearLayout filters = TvUi.row(c); filters.setPadding(0, d(18), 0, d(14));
        all = TvUi.button(c, "Tümü", () -> { favorites = false; render(); }); TvUi.addButton(filters, all);
        favoritesButton = TvUi.button(c, "Favoriler", () -> { favorites = true; render(); }); TvUi.addButton(filters, favoritesButton);
        groups = TvUi.button(c, "Gruplar", actions::groups); TvUi.addButton(filters, groups);
        search = new EditText(c); search.setSingleLine(); search.setTextSize(14); search.setTextColor(TvUi.TEXT);
        search.setHintTextColor(TvUi.MUTED); search.setHint("Kanal veya film ara"); search.setPadding(d(16), 0, d(12), 0);
        search.setBackground(TvUi.shape(c, TvUi.SURFACE, 22, 0));
        search.setOnFocusChangeListener((v, focused) -> search.setBackground(TvUi.shape(c, TvUi.SURFACE, 22, focused ? TvUi.TEXT : 0)));
        filters.addView(search, new LayoutParams(0, d(42), 1)); addView(filters);
        FrameLayout content = new FrameLayout(c); addView(content, new LayoutParams(-1, 0, 1));
        list = new ListView(c); list.setDivider(new android.graphics.drawable.ColorDrawable(0)); list.setDividerHeight(d(8));
        list.setSelector(TvUi.shape(c, 0x223d6baf, 14, TvUi.TEXT)); list.setDrawSelectorOnTop(true); list.setVerticalScrollBarEnabled(false);
        adapter = new BaseAdapter() {
            public int getCount() { return visible.size(); }
            public Object getItem(int n) { return visible.get(n); }
            public long getItemId(int n) { return n; }
            public View getView(int n, View recycled, ViewGroup parent) {
                LinearLayout row; TextView marker, name, info, favorite;
                if (recycled instanceof LinearLayout) {
                    row = (LinearLayout)recycled; TextView[] holder = (TextView[])row.getTag();
                    marker = holder[0]; name = holder[1]; info = holder[2]; favorite = holder[3];
                } else {
                    row = TvUi.row(c); row.setBackground(TvUi.shape(c, TvUi.SURFACE, 14, 0)); row.setPadding(d(18), d(12), d(18), d(12)); row.setMinimumHeight(d(70));
                    marker = TvUi.text(c, "▶", 22, TvUi.ACCENT, false); row.addView(marker, new LayoutParams(d(48), -2));
                    LinearLayout names = new LinearLayout(c); names.setOrientation(VERTICAL);
                    name = TvUi.text(c, "", 17, TvUi.TEXT, true); name.setSingleLine(); name.setEllipsize(TextUtils.TruncateAt.END);
                    info = TvUi.text(c, "", 11, TvUi.MUTED, false); info.setSingleLine(); info.setPadding(0, d(3), 0, 0);
                    names.addView(name); names.addView(info); row.addView(names, new LayoutParams(0, -2, 1));
                    favorite = TvUi.text(c, "", 20, TvUi.ACCENT, false); row.addView(favorite);
                    row.setTag(new TextView[]{marker, name, info, favorite});
                }
                MediaEntry entry = visible.get(n); name.setText(entry.title);
                info.setText(entry.group.isEmpty() ? "Kütüphane" : entry.group); favorite.setText(entry.favorite ? "★" : "");
                row.setContentDescription(entry.title + (entry.favorite ? ", favori" : "") + ", " + info.getText()); return row;
            }
        };
        list.setAdapter(adapter); list.setOnItemClickListener((p, v, n, id) -> actions.open(visible.get(n)));
        list.setOnItemLongClickListener((p, v, n, id) -> { actions.edit(visible.get(n)); return true; }); content.addView(list, new FrameLayout.LayoutParams(-1, -1));
        empty = new LinearLayout(c); empty.setOrientation(VERTICAL); empty.setGravity(Gravity.CENTER);
        emptyTitle = TvUi.text(c, "Bu akşam ne izleyelim?", 25, TvUi.TEXT, true); emptyTitle.setGravity(Gravity.CENTER); empty.addView(emptyTitle);
        emptyHint = TvUi.text(c, "Bağlantını gönder. Kanal listeni ekle. İzlemeye başla.", 13, 0xffafc2d5, false);
        emptyHint.setGravity(Gravity.CENTER); emptyHint.setPadding(0, d(7), 0, d(20)); empty.addView(emptyHint);
        LinearLayout choices = TvUi.row(c); choices.setGravity(Gravity.CENTER);
        View phoneCard = entryCard(c, "▶", "Telefondan gönder", "QR kodunu aç · iPhone ile paylaş", 0xff355d80, actions::phone);
        View listCard = entryCard(c, "≡", "Kanal listesi ekle", "M3U veya yayın bağlantısı", 0xff3d486c, actions::input);
        edits.add(phoneCard); edits.add(listCard);
        LayoutParams cp = new LayoutParams(d(270), d(127)); cp.rightMargin = d(16); choices.addView(phoneCard, cp);
        choices.addView(listCard, new LayoutParams(d(270), d(127))); empty.addView(choices);
        content.addView(empty, new FrameLayout.LayoutParams(-1, -1));
        TextView hint = TvUi.text(c, "OK  Oynat     ·     Uzun OK  Favori / kaldır     ·     Oynatıcıda Menü  Ses, altyazı ve kalite", 10, TvUi.MUTED, false);
        hint.setPadding(0, d(12), 0, 0); addView(hint);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { ui.removeCallbacks(searchChanged); ui.postDelayed(searchChanged, 150); }
            public void afterTextChanged(Editable s) { }
        });
        send.requestFocus();
    }
    private View entryCard(Context c, String icon, String title, String hint, int color, Runnable action) {
        LinearLayout card = new LinearLayout(c); card.setOrientation(VERTICAL); card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(d(22), 0, d(20), 0); card.setFocusable(true); card.setClickable(true);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, TvUi.shape(c, color, 18, 0xffe6f3ff));
        GradientDrawable glass = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{color, 0x50233548});
        glass.setCornerRadius(d(18)); glass.setStroke(d(1), 0x28ffffff); states.addState(new int[]{}, glass); card.setBackground(states);
        TextView symbol = TvUi.text(c, icon, 25, 0xffd2e7fa, false); card.addView(symbol);
        TextView name = TvUi.text(c, title, 18, TvUi.TEXT, true); name.setPadding(0, d(6), 0, d(4)); card.addView(name);
        card.addView(TvUi.text(c, hint, 11, 0xffb6cbdf, false));
        card.setContentDescription(title); card.setOnClickListener(v -> action.run()); return card;
    }
    private int d(float n) { return TvUi.dp(getContext(), n); }
    void showLibrary(LibraryIndex next) { index = next; render(); }
    void working(boolean busy) { for (View control : edits) { control.setEnabled(!busy); control.setAlpha(busy ? .45f : 1f); } if (busy) status("Kütüphane hazırlanıyor…"); }
    @Override protected void onDetachedFromWindow() { ui.removeCallbacksAndMessages(null); super.onDetachedFromWindow(); }
    void status(String text) { status.setText(text); }
    void group(String selected) { group = selected; render(); }
    private void render() {
        visible = index.filter(favorites, group, search.getText().toString());
        status.setText(visible.size() + " kayıt" + (favorites ? " · Favoriler" : "") + (group.isEmpty() ? "" : " · " + group));
        all.setText(favorites ? "Tümü" : "✓ Tümü"); favoritesButton.setText(favorites ? "✓ Favoriler" : "Favoriler");
        groups.setText(group.isEmpty() ? "Gruplar" : group);
        boolean isEmpty = visible.isEmpty(); empty.setVisibility(isEmpty ? VISIBLE : GONE); list.setVisibility(isEmpty ? GONE : VISIBLE);
        emptyTitle.setText(index.entries().isEmpty() ? "Bu akşam ne izleyelim?" : "Burada henüz bir kayıt yok.");
        emptyHint.setText(index.entries().isEmpty() ? "Bağlantını gönder. Kanal listeni ekle. İzlemeye başla." : "Aramayı temizle veya farklı bir grup seç.");
        adapter.notifyDataSetChanged();
    }
}
