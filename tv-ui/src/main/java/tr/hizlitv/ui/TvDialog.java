package tr.hizlitv.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/** Native, reusable TV prompt. The caller owns its lifecycle and supplies all text/actions. */
public final class TvDialog extends Dialog {
    public interface Selection { void select(int index); }
    private final Activity activity;
    private final LinearLayout panel, buttons;
    private View firstFocus;
    private android.widget.EditText field;

    public TvDialog(Activity activity, String eyebrow, String title, String message) {
        super(activity, R.style.HizliTvDialogTheme); this.activity = activity;
        setCanceledOnTouchOutside(false);
        panel = new LinearLayout(activity); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(d(28), d(25), d(28), d(25));
        GradientDrawable glass = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xff243b52, 0xff142130});
        glass.setCornerRadius(d(22)); glass.setStroke(d(1), 0x4589adcc); panel.setBackground(glass);
        TextView label = TvUi.text(activity, eyebrow, 12, 0xffb6d3ed, true); panel.addView(label);
        TextView name = TvUi.text(activity, title, 27, TvUi.TEXT, true);
        name.setMaxLines(2); name.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = d(13); panel.addView(name, titleParams);
        if (!message.isEmpty()) {
            TextView detail = TvUi.text(activity, message, 14, 0xffb9cbdb, false);
            detail.setMaxLines(6); detail.setEllipsize(TextUtils.TruncateAt.END); detail.setLineSpacing(d(3), 1);
            LinearLayout.LayoutParams details = new LinearLayout.LayoutParams(-1, -2);
            details.topMargin = d(12); panel.addView(detail, details);
        }
        buttons = TvUi.row(activity);
        LinearLayout.LayoutParams actions = new LinearLayout.LayoutParams(-1, d(48));
        actions.topMargin = d(25); panel.addView(buttons, actions); setContentView(panel);
    }

    public TvDialog action(String label, Runnable callback) {
        Button button = TvUi.button(activity, label, () -> { dismiss(); if (callback != null) callback.run(); });
        button.setSingleLine(); button.setEllipsize(TextUtils.TruncateAt.END); button.setFocusableInTouchMode(true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1);
        if (buttons.getChildCount() > 0) p.leftMargin = d(10);
        buttons.addView(button, p); if (firstFocus == null) firstFocus = button; return this;
    }

    public TvDialog choices(String[] labels, Selection selection) {
        ListView list = new ListView(activity); list.setBackgroundColor(Color.TRANSPARENT); list.setFocusableInTouchMode(true);
        list.setDivider(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)); list.setDividerHeight(d(5));
        list.setSelector(TvUi.shape(activity, 0x55547e9e, 12, TvUi.TEXT)); list.setDrawSelectorOnTop(true);
        list.setVerticalScrollBarEnabled(false);
        list.setAdapter(new BaseAdapter() {
            public int getCount() { return labels.length; }
            public Object getItem(int n) { return labels[n]; }
            public long getItemId(int n) { return n; }
            public View getView(int n, View recycled, ViewGroup parent) {
                TextView row = recycled instanceof TextView ? (TextView) recycled : TvUi.text(activity, "", 15, TvUi.TEXT, false);
                row.setText(labels[n]); row.setSingleLine(); row.setEllipsize(TextUtils.TruncateAt.END);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL); row.setPadding(d(15), 0, d(15), 0);
                row.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1, d(48))); return row;
            }
        });
        list.setOnItemClickListener((p, v, n, id) -> { dismiss(); selection.select(n); });
        // Some TV builds use the first confirm key only to resurrect list selection.
        // Handle a complete confirm press here so a visible row always opens with one OK.
        list.setOnKeyListener((v, key, event) -> {
            if (key != KeyEvent.KEYCODE_DPAD_CENTER && key != KeyEvent.KEYCODE_ENTER && key != KeyEvent.KEYCODE_NUMPAD_ENTER) return false;
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled() && labels.length > 0) {
                int selected = list.getSelectedItemPosition();
                dismiss(); selection.select(selected < 0 ? 0 : selected);
            }
            return true;
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, d(53) * Math.min(5, labels.length));
        p.topMargin = d(17); panel.addView(list, panel.indexOfChild(buttons), p);
        firstFocus = list; return this;
    }

    /** Adds a text field (first focus); read it with {@link #text()} inside an action. */
    public TvDialog field(String hint) {
        field = new android.widget.EditText(activity); field.setTextSize(16); field.setTextColor(TvUi.TEXT); field.setHintTextColor(TvUi.MUTED);
        field.setHint(hint); field.setMaxLines(5); field.setPadding(d(16), d(12), d(16), d(12));
        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, TvUi.shape(activity, TvUi.SURFACE, 14, TvUi.TEXT));
        states.addState(new int[]{}, TvUi.shape(activity, TvUi.SURFACE, 14, 0)); field.setBackground(states);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = d(17);
        panel.addView(field, panel.indexOfChild(buttons), p); firstFocus = field; return this;
    }
    public String text() { return field == null ? "" : field.getText().toString(); }
    /** Adds caller-built content (for example a QR image) above the buttons. */
    public TvDialog content(View view) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = d(17);
        panel.addView(view, panel.indexOfChild(buttons), p); return this;
    }

    @Override public void show() {
        super.show(); Window window = getWindow();
        if (window != null) {
            window.getDecorView().setPadding(0, 0, 0, 0); window.getDecorView().setSystemUiVisibility(5894);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            int available = activity.getResources().getDisplayMetrics().widthPixels - d(64);
            window.setLayout(Math.max(1, Math.min(d(740), available)), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (firstFocus != null) {
            // Phone/touch interaction must not leave a TV list without an active row.
            firstFocus.requestFocusFromTouch();
            if (firstFocus instanceof ListView) ((ListView) firstFocus).setSelection(0);
        }
    }
    private int d(float value) { return TvUi.dp(activity, value); }
}
