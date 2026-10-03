package com.golda.patchertiktok;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/** Shows the seek bar on every regular video, including clips shorter than 30 seconds. */
final class Seekbar {
    private static final String AWEME = "com.ss.android.ugc.aweme.feed.model.Aweme";
    /** Log message inside the controller's "can show seek bar" decision. */
    private static final String FINGERPRINT = "can not show seekbar, state: 1, not in resume";
    private static final String ELIGIBLE = "com.golda.patchertiktok.seekbarEligible";

    private Seekbar() { }

    static void install(ClassLoader loader) {
        Class<?> aweme = Hooks.findClass(AWEME, loader);
        int decision = 0;
        int shortVideo = 0;
        for (String entry : Discovery.methodsUsing(FINGERPRINT)) {
            String[] parts = entry.split("#", 2);
            Class<?> controller = Hooks.findClass(parts[0], loader);
            if (controller == null || aweme == null) continue;
            List<Method> shortMethods = new ArrayList<>();
            for (Method method : controller.getDeclaredMethods()) {
                Class<?>[] p = method.getParameterTypes();
                if (method.getName().equals(parts[1]) && method.getReturnType() == boolean.class
                        && p.length == 1 && p[0] == aweme) {
                    Hooks.hook(method, new Hooks.Hook() {
                        @Override protected void after(Hooks.Call param) {
                            if (Prefs.on(Prefs.SEEKBAR) && !Boolean.TRUE.equals(param.getResult())
                                    && eligible(param.args[0])) param.setResult(true);
                        }
                    });
                    decision++;
                } else if (method.getReturnType() == int.class && p.length == 1 && p[0] == boolean.class) {
                    shortMethods.add(method);
                }
            }
            // The short-clip threshold is the controller's only (boolean) -> int method.
            if (shortMethods.size() == 1) {
                Hooks.hook(shortMethods.get(0), new Hooks.Hook() {
                    @Override protected void after(Hooks.Call param) {
                        if (Prefs.on(Prefs.SEEKBAR)) param.setResult(0);
                    }
                });
                shortVideo++;
            }
        }
        int showType = hookShowType(loader);
        RuntimeLog.log("seekbar: decision=" + decision + " short=" + shortVideo + " showType=" + showType);
    }

    /** Seek bar views with setSeekBarShowType(int); types 3 and 4 hide it on short clips. */
    private static int hookShowType(ClassLoader loader) {
        Class<?> assem = Hooks.findClass(
                "com.bytedance.tiktok.homepage.mainpagefragment.assem.MainPageSeekAssem", loader);
        if (assem == null) return 0;
        int count = 0;
        List<Method> hooked = new ArrayList<>();
        for (Field field : assem.getDeclaredFields()) {
            for (Method method : field.getType().getDeclaredMethods()) {
                Class<?>[] p = method.getParameterTypes();
                if (!"setSeekBarShowType".equals(method.getName()) || p.length != 1 || p[0] != int.class
                        || hooked.contains(method)) continue;
                Hooks.hook(method, new Hooks.Hook() {
                    @Override protected void before(Hooks.Call param) {
                        if (Prefs.on(Prefs.SEEKBAR) && param.args[0] instanceof Integer) {
                            param.args[0] = SeekbarPolicy.normalizeShowType((Integer) param.args[0]);
                        }
                    }
                });
                hooked.add(method);
                count++;
            }
        }
        return count;
    }

    private static boolean eligible(Object aweme) {
        if (aweme == null) return false;
        Object cached = Hooks.getExtra(aweme, ELIGIBLE);
        if (cached instanceof Boolean) return (Boolean) cached;
        boolean result;
        try {
            Object type = Reflect.call(aweme, "getAwemeType");
            int awemeType = type instanceof Number ? ((Number) type).intValue() : 0;
            boolean photo = awemeType == 150 || Reflect.call(aweme, "getPhotoModeImageInfo") != null
                    || Reflect.call(aweme, "getPhotoModeTextInfo") != null;
            result = SeekbarPolicy.shouldForceShow(false, Reflect.call(aweme, "getVideo") != null,
                    FeedFilter.isAd(aweme), awemeType == 101, photo);
        } catch (Throwable ignored) {
            result = false;
        }
        Hooks.setExtra(aweme, ELIGIBLE, result);
        return result;
    }
}
