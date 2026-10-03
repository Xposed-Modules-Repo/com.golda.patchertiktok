package com.golda.patchertiktok;

import android.app.Activity;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


/**
 * Last line of defence: an ad inserted into the feed after filtering is skipped with a swipe
 * once its first frame renders, but only while the main feed activity is in the foreground.
 */
final class RenderedAdSkip {
    private static final String PANEL = "com.ss.android.ugc.aweme.feed.panel.BaseListFragmentPanel";
    private static final String AWEME = "com.ss.android.ugc.aweme.feed.model.Aweme";
    private static final String SKIPPED = "com.golda.patchertiktok.adSkipped";

    private RenderedAdSkip() { }

    static void install(ClassLoader loader) {
        Class<?> panel = Hooks.findClass(PANEL, loader);
        if (panel == null) return;
        Method current = currentAweme(panel);
        if (current == null) return;
        current.setAccessible(true);
        int hooks = 0;
        for (Method method : panel.getDeclaredMethods()) {
            if (!"onRenderFirstFrame".equals(method.getName()) || method.getReturnType() != void.class) continue;
            Hooks.hook(method, new Hooks.Hook() {
                @Override protected void after(Hooks.Call param) {
                    if (!Prefs.on(Prefs.ADS)) return;
                    try {
                        Object aweme = current.invoke(param.thisObject);
                        if (FeedFilter.isAd(aweme)) skip(param.thisObject, aweme);
                    } catch (Throwable ignored) { }
                }
            });
            hooks++;
        }
        RuntimeLog.log("rendered ad skip: hooks=" + hooks);
    }

    private static Method currentAweme(Class<?> panel) {
        for (Method method : panel.getDeclaredMethods()) {
            if ("getCurrentAweme".equals(method.getName()) && method.getParameterTypes().length == 0
                    && AWEME.equals(method.getReturnType().getName())) return method;
        }
        // Obfuscated builds expose the pager's current item through final aliases of one getter.
        for (Method method : panel.getDeclaredMethods()) {
            if (method.getParameterTypes().length == 0 && AWEME.equals(method.getReturnType().getName())
                    && Modifier.isPublic(method.getModifiers()) && Modifier.isFinal(method.getModifiers())) {
                return method;
            }
        }
        return null;
    }

    private static void skip(Object panel, Object aweme) {
        if (aweme == null || Boolean.TRUE.equals(Hooks.getExtra(aweme, SKIPPED))) return;
        Activity activity = null;
        Object value = Reflect.get(panel, "activity");
        if (value instanceof Activity) activity = (Activity) value;
        if (activity == null && Reflect.call(panel, "getActivity") instanceof Activity) {
            activity = (Activity) Reflect.call(panel, "getActivity");
        }
        if (activity == null || activity.isFinishing() || !activity.hasWindowFocus()) return;
        Hooks.setExtra(aweme, SKIPPED, true);
        View decor = activity.getWindow().getDecorView();
        decor.postDelayed(() -> swipe(decor), 80L);
    }

    private static void swipe(View view) {
        int width = view.getWidth();
        int height = view.getHeight();
        if (width <= 0 || height <= 0) return;
        float x = width / 2f;
        float startY = height * 0.80f;
        float endY = height * 0.20f;
        long down = SystemClock.uptimeMillis();
        touch(view, MotionEvent.ACTION_DOWN, x, startY, down, down);
        for (int step = 1; step <= 10; step++) {
            touch(view, MotionEvent.ACTION_MOVE, x, startY + (endY - startY) * step / 10f, down, down + step * 6L);
        }
        touch(view, MotionEvent.ACTION_UP, x, endY, down, down + 66L);
    }

    private static void touch(View view, int action, float x, float y, long down, long time) {
        MotionEvent event = MotionEvent.obtain(down, time, action, x, y, 0);
        try {
            view.dispatchTouchEvent(event);
        } finally {
            event.recycle();
        }
    }
}
