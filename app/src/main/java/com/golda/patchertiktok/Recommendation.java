package com.golda.patchertiktok;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Keeps recommendations local while the region spoof is active: the SIM says another
 * country, but feed requests carry the device's real language, region and carrier.
 */
final class Recommendation {
    private Recommendation() { }

    static void install(ClassLoader loader) {
        hookContentLanguage(loader);
        hookFeedRequests(loader);
    }

    private static void hookContentLanguage(ClassLoader loader) {
        XC_MethodReplacement language = new XC_MethodReplacement() {
            @Override protected Object replaceHookedMethod(MethodHookParam param) {
                return DeviceSignals.current().language;
            }
        };
        Class<?> service = XposedHelpers.findClassIfExists(
                "com.ss.android.ugc.aweme.contentlanguage.ContentLanguageServiceImpl", loader);
        if (service != null) {
            try {
                XposedHelpers.findAndHookMethod(service, "getContentLanguage", language);
                XposedHelpers.findAndHookMethod(service, "getLanguage", new XC_MethodReplacement() {
                    @Override protected Object replaceHookedMethod(MethodHookParam param) {
                        return new ArrayList<>(Collections.singletonList(DeviceSignals.current().language));
                    }
                });
            } catch (Throwable error) {
                RuntimeLog.log("content language unavailable: " + error.getClass().getSimpleName());
            }
        }
        Class<?> guide = XposedHelpers.findClassIfExists(
                "com.ss.android.ugc.aweme.contentlanguage.api.ContentLanguageGuideServiceImpl", loader);
        if (guide != null) {
            try {
                XposedHelpers.findAndHookMethod(guide, "getContentLanguage", language);
            } catch (Throwable ignored) { }
        }
    }

    private static void hookFeedRequests(ClassLoader loader) {
        try {
            Class<?> request = XposedHelpers.findClass("com.bytedance.retrofit2.client.Request", loader);
            Field url = request.getDeclaredField("url");
            if (url.getType() != String.class) return;
            url.setAccessible(true);
            XposedBridge.hookAllConstructors(request, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (param.hasThrowable()) return;
                    try {
                        String original = (String) url.get(param.thisObject);
                        if (original == null || !original.contains("/feed")) return;
                        String rewritten = RecommendationUrlPolicy.rewrite(original, DeviceSignals.current());
                        // Constructors run before Request caches its parsed URI.
                        if (!original.equals(rewritten)) url.set(param.thisObject, rewritten);
                    } catch (Throwable ignored) { }
                }
            });
        } catch (Throwable error) {
            RuntimeLog.log("feed request rewrite unavailable: " + error.getClass().getSimpleName());
        }
    }
}
