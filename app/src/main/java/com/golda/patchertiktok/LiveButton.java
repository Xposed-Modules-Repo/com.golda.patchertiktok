package com.golda.patchertiktok;

import android.view.View;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** Hides the LIVE entry in the top-left corner of the home feed. */
final class LiveButton {
    private static final String GENERATOR = "com.bytedance.tiktok.homepage.mainfragment.toolbar.LiveIconGenerator";

    private LiveButton() { }

    static void install(ClassLoader loader) {
        Class<?> generator = XposedHelpers.findClassIfExists(GENERATOR, loader);
        if (generator == null) {
            RuntimeLog.log("LIVE button: generator not found");
            return;
        }
        for (Method method : generator.getDeclaredMethods()) {
            Class<?>[] p = method.getParameterTypes();
            if ("enabled".equals(method.getName()) && method.getReturnType() == boolean.class && p.length == 0) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (Prefs.on(Prefs.LIVE)) param.setResult(false);
                    }
                });
            } else if (View.class.isAssignableFrom(method.getReturnType()) && p.length == 1
                    && "android.content.Context".equals(p[0].getName())) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (!Prefs.on(Prefs.LIVE)) return;
                        hide(param.getResult());
                        hideIcons(param.thisObject);
                    }
                });
            } else if (method.getReturnType() == void.class && p.length == 1 && p[0] == boolean.class) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        if (Prefs.on(Prefs.LIVE)) param.args[0] = false;
                    }

                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (Prefs.on(Prefs.LIVE)) hideIcons(param.thisObject);
                    }
                });
            }
        }
        XC_MethodHook keepHidden = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (Prefs.on(Prefs.LIVE)) hideIcons(param.thisObject);
            }
        };
        XposedBridge.hookAllMethods(generator, "onCreate", keepHidden);
        XposedBridge.hookAllMethods(generator, "onResume", keepHidden);
        XposedBridge.hookAllMethods(generator, "onLiveIconEntranceEnable", keepHidden);
    }

    private static void hideIcons(Object generator) {
        if (generator == null) return;
        for (Class<?> type = generator.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!ImageView.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    hide(field.get(generator));
                } catch (Throwable ignored) { }
            }
        }
    }

    private static void hide(Object value) {
        if (!(value instanceof View)) return;
        View view = (View) value;
        view.setVisibility(View.GONE);
        view.setClickable(false);
        view.setLongClickable(false);
    }
}
