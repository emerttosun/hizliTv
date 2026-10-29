package tr.hizlitv.ui;

import android.app.Activity;
import android.content.Context;

/** Applies the app preference without modifying the TV's system language. */
public abstract class LocalizedActivity extends Activity {
    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(Language.wrap(base)); }
}
