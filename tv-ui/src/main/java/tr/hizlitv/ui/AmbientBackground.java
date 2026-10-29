package tr.hizlitv.ui;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Static, GPU-friendly color atmosphere. Shaders are cached; no bitmap or blur loop. */
public final class AmbientBackground extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Shader base, blue, violet;
    @Override protected void onBoundsChange(Rect r) {
        float w = Math.max(1, r.width()), h = Math.max(1, r.height());
        base = new LinearGradient(0, 0, w, h, new int[]{0xff182637, 0xff101925, 0xff11151f}, null, Shader.TileMode.CLAMP);
        blue = new RadialGradient(w * .82f, h * .05f, w * .68f, new int[]{0x253574a0, 0x003574a0}, null, Shader.TileMode.CLAMP);
        violet = new RadialGradient(w * .02f, h * 1.1f, w * .58f, new int[]{0x252c294d, 0x002c294d}, null, Shader.TileMode.CLAMP);
    }
    @Override public void draw(Canvas c) {
        if (base == null) onBoundsChange(getBounds());
        paint.setShader(base); c.drawRect(getBounds(), paint);
        paint.setShader(blue); c.drawRect(getBounds(), paint);
        paint.setShader(violet); c.drawRect(getBounds(), paint); paint.setShader(null);
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
