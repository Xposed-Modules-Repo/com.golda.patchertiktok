package com.golda.patchertiktok;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.PathInterpolator;

/** A switch drawn like TikTok's own toggle. The owning row handles clicks. */
final class Toggle extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Palette palette;
    private ValueAnimator animator;
    private boolean checked;
    private float progress;

    Toggle(Context context, Palette palette) {
        super(context);
        this.palette = palette;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    boolean checked() { return checked; }

    void setChecked(boolean value, boolean animate) {
        if (animator != null) animator.cancel();
        checked = value;
        float target = value ? 1f : 0f;
        if (!animate || !isAttachedToWindow()) {
            progress = target;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(progress, target);
        animator.setDuration(200);
        animator.setInterpolator(new PathInterpolator(0.2f, 0f, 0f, 1f));
        animator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        float density = getResources().getDisplayMetrics().density;
        setMeasuredDimension(Math.round(48 * density), Math.round(27 * density));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        paint.setColor(blend(palette.trackOff, palette.trackOn, progress));
        rect.set(0, 0, width, height);
        canvas.drawRoundRect(rect, height / 2f, height / 2f, paint);
        float inset = height * 0.1f;
        float radius = height / 2f - inset;
        float x = inset + radius + (width - 2 * (inset + radius)) * progress;
        paint.setColor(palette.thumb);
        paint.setShadowLayer(inset, 0, inset / 3f, 0x33000000);
        canvas.drawCircle(x, height / 2f, radius, paint);
        paint.clearShadowLayer();
    }

    private static int blend(int from, int to, float amount) {
        int a = (int) (((from >>> 24) & 255) + (((to >>> 24) & 255) - ((from >>> 24) & 255)) * amount);
        int r = (int) (((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * amount);
        int g = (int) (((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * amount);
        int b = (int) ((from & 255) + ((to & 255) - (from & 255)) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
