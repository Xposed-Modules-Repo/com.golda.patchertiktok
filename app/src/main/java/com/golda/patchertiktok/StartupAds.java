package com.golda.patchertiktok;

import java.lang.reflect.Method;


/** Splash and TopView ads shown when TikTok opens. A/B overrides disable them at the source. */
final class StartupAds {
    private static final String[] PRELOAD_TASKS = {
            "com.bytedance.ies.ugc.aweme.commercialize.splash.SplashAdManagerPreloadTask",
            "com.bytedance.ies.ugc.aweme.commercialize.splash.topview.TopViewPreloadTask",
            "com.bytedance.ies.ugc.aweme.commercialize.splash.topview.TopViewPreloadJsonTask",
            "com.bytedance.ies.ugc.aweme.commercialize.splash.topview.RealTimeSplashTask",
    };

    private StartupAds() { }

    static void install(ClassLoader loader) {
        if (!Prefs.on(Prefs.ADS)) return;
        int tasks = 0;
        Hooks.Hook skip = new Hooks.Hook() {
            @Override protected void before(Hooks.Call param) { param.setResult(null); }
        };
        for (String name : PRELOAD_TASKS) {
            Class<?> task = Hooks.findClass(name, loader);
            if (task == null) continue;
            for (Method method : task.getDeclaredMethods()) {
                if ("run".equals(method.getName()) && method.getReturnType() == void.class) {
                    Hooks.hook(method, skip);
                    tasks++;
                }
            }
        }
        // The cold-boot "splash enabled" getter reads the cached splash_ad_enable flag.
        int gates = 0;
        for (String entry : Discovery.methodsUsing("splash_ad_enable")) {
            String[] parts = entry.split("#", 2);
            Class<?> owner = Hooks.findClass(parts[0], loader);
            if (owner == null) continue;
            for (Method method : owner.getDeclaredMethods()) {
                if (method.getName().equals(parts[1]) && method.getReturnType() == boolean.class
                        && method.getParameterTypes().length == 0) {
                    Hooks.hook(method, new Hooks.Hook() {
                        @Override protected void before(Hooks.Call param) { param.setResult(false); }
                    });
                    gates++;
                }
            }
        }
        RuntimeLog.log("startup ads: tasks=" + tasks + " gates=" + gates);
    }
}
