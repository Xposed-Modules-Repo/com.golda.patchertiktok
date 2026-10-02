package com.golda.patchertiktok;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Small stroke icons drawn in code, sized on a 24-unit grid like TikTok's icons. */
final class Glyph extends Drawable {
    enum Kind { BACK, CHEVRON, CHECK, SEARCH }

    private final Kind kind;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final int size;

    Glyph(Kind kind, int color, int sizePx) {
        this.kind = kind;
        this.size = sizePx;
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override public int getIntrinsicWidth() { return size; }

    @Override public int getIntrinsicHeight() { return size; }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float unit = Math.min(bounds.width(), bounds.height()) / 24f;
        canvas.save();
        canvas.translate(bounds.left + (bounds.width() - 24 * unit) / 2f, bounds.top + (bounds.height() - 24 * unit) / 2f);
        canvas.scale(unit, unit);
        path.reset();
        switch (kind) {
            case BACK:
                paint.setStrokeWidth(2.2f);
                path.moveTo(20.5f, 12f);
                path.lineTo(3.5f, 12f);
                path.moveTo(10.5f, 4.5f);
                path.lineTo(3.2f, 12f);
                path.lineTo(10.5f, 19.5f);
                break;
            case CHEVRON:
                paint.setStrokeWidth(1.9f);
                path.moveTo(9f, 5f);
                path.lineTo(16f, 12f);
                path.lineTo(9f, 19f);
                break;
            case CHECK:
                paint.setStrokeWidth(2.4f);
                path.moveTo(4.5f, 12.5f);
                path.lineTo(9.5f, 17.5f);
                path.lineTo(19.5f, 6.5f);
                break;
            case SEARCH:
                paint.setStrokeWidth(2f);
                path.addCircle(10.5f, 10.5f, 6.5f, Path.Direction.CW);
                path.moveTo(15.5f, 15.5f);
                path.lineTo(20.5f, 20.5f);
                break;
        }
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }

    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }

    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
