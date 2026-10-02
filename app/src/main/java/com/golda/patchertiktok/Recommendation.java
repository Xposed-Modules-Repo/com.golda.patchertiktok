package com.golda.patchertiktok;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Collections;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

/**
 * Keeps the content language on the device language while the region spoof is active,
 * so a German SIM does not switch recommendations to German videos.
 */
final class Recommendation {
    private Recommendation() { }

    static String language() {
        String language = Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage();
        return language.isEmpty() ? "en" : language;
    }

    static void install(ClassLoader loader) {
        XC_MethodReplacement language = new XC_MethodReplacement() {
            @Override protected Object replaceHookedMethod(MethodHookParam param) {
                return language();
            }
        };
        Class<?> service = XposedHelpers.findClassIfExists(
                "com.ss.android.ugc.aweme.contentlanguage.ContentLanguageServiceImpl", loader);
        if (service != null) {
            try {
                XposedHelpers.findAndHookMethod(service, "getContentLanguage", language);
                XposedHelpers.findAndHookMethod(service, "getLanguage", new XC_MethodReplacement() {
                    @Override protected Object replaceHookedMethod(MethodHookParam param) {
                        return new ArrayList<>(Collections.singletonList(language()));
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
}
