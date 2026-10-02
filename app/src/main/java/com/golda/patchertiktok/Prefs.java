package com.golda.patchertiktok;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Settings stored inside TikTok's own data directory. Hooks read cached values,
 * so a toggle applies immediately where the hooked code is consulted again.
 */
final class Prefs {
    static final String FILE = "tiktokpatchxposed";

    static final String ADS = "block_ads";
    static final String LIVE = "hide_live";
    static final String ACQUAINTANCES = "hide_acquaintances";
    static final String SEEKBAR = "seekbar";
    static final String NO_WATERMARK = "no_watermark";
    static final String DOWNLOAD_ANY = "download_any";
    static final String SCREENSHOTS = "allow_screenshots";
    static final String COMMENT_REPOSTS = "comment_reposts";
    static final String NEW_PROFILE = "new_profile";
    static final String EXTRA_FEATURES = "extra_features";
    static final String CLEAN_LINKS = "clean_links";
    static final String REGION = "region";
    static final String AUTO_STREAK = "auto_streak";
    /** Germany keeps the behaviour of earlier releases; users can pick another country or turn it off. */
    static final String DEFAULT_REGION = "de";

    /** Keys whose hooks only take effect when TikTok starts. */
    static final String[] RESTART_KEYS = {COMMENT_REPOSTS, NEW_PROFILE, EXTRA_FEATURES, REGION, ADS};

    private static final Map<String, Boolean> DEFAULTS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> FLAGS = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile SharedPreferences preferences;
    private static volatile String region = DEFAULT_REGION;
    private static volatile String startSnapshot;

    static {
        DEFAULTS.put(ADS, true);
        DEFAULTS.put(LIVE, true);
        DEFAULTS.put(ACQUAINTANCES, true);
        DEFAULTS.put(SEEKBAR, true);
        DEFAULTS.put(NO_WATERMARK, true);
        DEFAULTS.put(DOWNLOAD_ANY, true);
        DEFAULTS.put(SCREENSHOTS, true);
        DEFAULTS.put(COMMENT_REPOSTS, true);
        DEFAULTS.put(NEW_PROFILE, true);
        DEFAULTS.put(EXTRA_FEATURES, true);
        DEFAULTS.put(CLEAN_LINKS, true);
        // Automated messages are opt-in only.
        DEFAULTS.put(AUTO_STREAK, false);
    }

    private Prefs() { }

    static synchronized void load(Context context) {
        if (preferences != null || context == null) return;
        SharedPreferences loaded = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        for (String key : DEFAULTS.keySet()) FLAGS.put(key, loaded.getBoolean(key, DEFAULTS.get(key)));
        region = Regions.normalize(loaded.getString(REGION, DEFAULT_REGION));
        preferences = loaded;
        startSnapshot = snapshot();
    }

    /** True when a setting that is applied only at startup differs from the running process. */
    static boolean restartPending() {
        return startSnapshot != null && !startSnapshot.equals(snapshot());
    }

    static boolean loaded() { return preferences != null; }

    static boolean on(String key) {
        Boolean value = FLAGS.get(key);
        if (value != null) return value;
        Boolean fallback = DEFAULTS.get(key);
        return fallback != null && fallback;
    }

    static void set(String key, boolean value) {
        FLAGS.put(key, value);
        SharedPreferences current = preferences;
        if (current != null) current.edit().putBoolean(key, value).apply();
        notifyChanged();
    }

    /** Selected spoof region as lower-case ISO code, or empty when disabled. */
    static String region() { return region; }

    static void setRegion(String iso) {
        region = Regions.normalize(iso);
        SharedPreferences current = preferences;
        if (current != null) current.edit().putString(REGION, region).apply();
        notifyChanged();
    }

    static String snapshot() {
        StringBuilder value = new StringBuilder(region);
        for (String key : RESTART_KEYS) value.append(on(key) ? '1' : '0');
        return value.toString();
    }

    static void listen(Runnable listener) { LISTENERS.addIfAbsent(listener); }

    static void unlisten(Runnable listener) { LISTENERS.remove(listener); }

    private static void notifyChanged() {
        for (Runnable listener : LISTENERS) listener.run();
    }
}
