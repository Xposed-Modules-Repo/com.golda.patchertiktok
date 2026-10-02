package com.golda.patchertiktok;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Version-independent lookup of obfuscated TikTok classes by string fingerprints.
 * Results are cached per TikTok build, so the dex scan runs once after each update.
 */
final class Discovery {
    private static final String FILE = "tiktokpatchxposed_index";
    private static final String NONE = "-";
    private static SharedPreferences cache;
    private static String build;
    private static ApplicationInfo info;
    private static DexFinder finder;

    private Discovery() { }

    static synchronized void init(Context context) {
        if (cache != null) return;
        info = context.getApplicationInfo();
        long version;
        try {
            android.content.pm.PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = android.os.Build.VERSION.SDK_INT >= 28 ? packageInfo.getLongVersionCode() : packageInfo.versionCode;
        } catch (Exception error) {
            version = 0;
        }
        build = version + ":" + new java.io.File(info.sourceDir).lastModified();
        cache = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        if (!build.equals(cache.getString("build", null))) {
            cache.edit().clear().putString("build", build).apply();
        }
    }

    /** Classes whose code loads {@code literal}, in dex order. */
    static synchronized List<String> classesUsing(String literal) {
        if (cache == null) return Collections.emptyList();
        String key = "c:" + literal;
        String saved = cache.getString(key, null);
        if (saved == null) {
            long start = System.currentTimeMillis();
            List<String> found = new ArrayList<>(finder().classesUsing(literal));
            saved = found.isEmpty() ? NONE : String.join(",", found);
            cache.edit().putString(key, saved).apply();
            RuntimeLog.log("indexed \"" + literal + "\" in " + (System.currentTimeMillis() - start) + " ms: " + saved);
        }
        return NONE.equals(saved) ? Collections.emptyList() : Arrays.asList(saved.split(","));
    }

    /** "class#method" entries for methods whose code loads {@code literal}. */
    static synchronized List<String> methodsUsing(String literal) {
        if (cache == null) return Collections.emptyList();
        String key = "m:" + literal;
        String saved = cache.getString(key, null);
        if (saved == null) {
            List<String> found = new ArrayList<>();
            for (DexFinder.Hit hit : finder().methodsUsing(literal)) found.add(hit.className + "#" + hit.methodName);
            saved = found.isEmpty() ? NONE : String.join(",", found);
            cache.edit().putString(key, saved).apply();
        }
        return NONE.equals(saved) ? Collections.emptyList() : Arrays.asList(saved.split(","));
    }

    /** "class#method" entries for the calls made from {@code className#methodName}. */
    static synchronized List<String> invokedBy(String className, String methodName) {
        if (cache == null) return Collections.emptyList();
        String key = "i:" + className + "#" + methodName;
        String saved = cache.getString(key, null);
        if (saved == null) {
            List<String> found = new ArrayList<>(new java.util.LinkedHashSet<>(finder().invokedBy(className, methodName)));
            saved = found.isEmpty() ? NONE : String.join(",", found);
            cache.edit().putString(key, saved).apply();
        }
        return NONE.equals(saved) ? Collections.emptyList() : Arrays.asList(saved.split(","));
    }

    static Class<?> firstClass(ClassLoader loader, String literal) {
        for (String name : classesUsing(literal)) {
            try {
                return Class.forName(name, false, loader);
            } catch (Throwable ignored) { }
        }
        return null;
    }

    private static DexFinder finder() {
        if (finder == null) {
            List<String> paths = new ArrayList<>();
            paths.add(info.sourceDir);
            if (info.splitSourceDirs != null) {
                for (String split : info.splitSourceDirs) {
                    // Configuration splits (abi, density, language) carry no code.
                    if (!split.contains("split_config.")) paths.add(split);
                }
            }
            finder = new DexFinder(paths);
        }
        return finder;
    }

    /** Releases the mapped dex files once startup lookups are done. */
    static synchronized void release() { finder = null; }
}
