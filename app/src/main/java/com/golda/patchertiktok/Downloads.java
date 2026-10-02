package com.golda.patchertiktok;

import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** Download permissions, watermark-free saving and screenshots. */
final class Downloads {
    private static final String ACL = "com.ss.android.ugc.aweme.feed.model.ACLCommonShare";

    private Downloads() { }

    static void install(ClassLoader loader) {
        Class<?> acl = XposedHelpers.findClassIfExists(ACL, loader);
        if (acl != null) {
            // code 0 = allowed, show type 2 = show the download button.
            override(acl, "getCode", Prefs.DOWNLOAD_ANY, 0);
            override(acl, "getShowType", Prefs.DOWNLOAD_ANY, 2);
            override(acl, "getTranscode", Prefs.NO_WATERMARK, 1);
        }
        installScreenshots();
    }

    private static void override(Class<?> type, String method, String key, int value) {
        XposedBridge.hookAllMethods(type, method, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (Prefs.on(key)) param.setResult(value);
            }
        });
    }

    private static void installScreenshots() {
        XposedHelpers.findAndHookMethod(Window.class, "setFlags", int.class, int.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (!Prefs.on(Prefs.SCREENSHOTS)) return;
                param.args[0] = (Integer) param.args[0] & ~WindowManager.LayoutParams.FLAG_SECURE;
            }
        });
        XposedHelpers.findAndHookMethod(SurfaceView.class, "setSecure", boolean.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (Prefs.on(Prefs.SCREENSHOTS)) param.args[0] = false;
            }
        });
    }
}
