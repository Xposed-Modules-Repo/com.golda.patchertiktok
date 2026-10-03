package com.golda.patchertiktok;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Collections;


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
        Hooks.Replace language = new Hooks.Replace() {
            @Override protected Object replace(Hooks.Call param) {
                return language();
            }
        };
        Class<?> service = Hooks.findClass(
                "com.ss.android.ugc.aweme.contentlanguage.ContentLanguageServiceImpl", loader);
        if (service != null) {
            try {
                Hooks.findAndHook(service, "getContentLanguage", language);
                Hooks.findAndHook(service, "getLanguage", new Hooks.Replace() {
                    @Override protected Object replace(Hooks.Call param) {
                        return new ArrayList<>(Collections.singletonList(language()));
                    }
                });
            } catch (Throwable error) {
                RuntimeLog.log("content language unavailable: " + error.getClass().getSimpleName());
            }
        }
        Class<?> guide = Hooks.findClass(
                "com.ss.android.ugc.aweme.contentlanguage.api.ContentLanguageGuideServiceImpl", loader);
        if (guide != null) {
            try {
                Hooks.findAndHook(guide, "getContentLanguage", language);
            } catch (Throwable ignored) { }
        }
    }
}
