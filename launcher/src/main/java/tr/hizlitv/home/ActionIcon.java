package tr.hizlitv.home;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Original small vector glyphs; no font-icon or network dependency. */
final class ActionIcon extends Drawable {
    private final HomeAction action;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private boolean focused;
    ActionIcon(HomeAction action) { this.action = action; }
    @Override public boolean isStateful() { return true; }
    @Override protected boolean onStateChange(int[] states) {
        boolean next = false; for (int state : states) if (state == android.R.attr.state_focused) next = true;
        if (focused == next) return false; focused = next; invalidateSelf(); return true;
    }
    @Override public void draw(Canvas canvas) {
        Rect r = getBounds(); int save = canvas.save(); canvas.translate(r.left, r.top); canvas.scale(r.width() / 24f, r.height() / 24f);
        paint.setColor(focused ? 0xff15283b : 0xffd2e0ef); paint.setStrokeWidth(1.7f); paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
        path.reset();
        switch (action) {
            case AIRPLAY:
                path.moveTo(7, 18); path.lineTo(3, 18); path.lineTo(3, 4); path.lineTo(21, 4); path.lineTo(21, 18); path.lineTo(17, 18); canvas.drawPath(path, paint);
                paint.setStyle(Paint.Style.FILL); path.reset(); path.moveTo(12, 13); path.lineTo(6, 21); path.lineTo(18, 21); path.close(); canvas.drawPath(path, paint); break;
            case PROCESSES:
                canvas.drawRoundRect(6, 6, 18, 18, 2, 2, paint); canvas.drawRect(9.5f, 9.5f, 14.5f, 14.5f, paint);
                for (int i = 0; i < 3; i++) { float p = 9 + i * 3; canvas.drawLine(p, 2.5f, p, 6, paint); canvas.drawLine(p, 18, p, 21.5f, paint); canvas.drawLine(2.5f, p, 6, p, paint); canvas.drawLine(18, p, 21.5f, p, paint); } break;
            case SETTINGS:
                canvas.drawCircle(12, 12, 6.5f, paint); canvas.drawCircle(12, 12, 2.5f, paint);
                for (int i = 0; i < 8; i++) { canvas.drawLine(12, 2, 12, 4, paint); canvas.rotate(45, 12, 12); } break;
            case STOCK_HOME:
                canvas.drawRoundRect(2, 3, 22, 17, 2, 2, paint); canvas.drawLine(8, 21, 16, 21, paint); canvas.drawLine(12, 17, 12, 21, paint); break;
        }
        canvas.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
