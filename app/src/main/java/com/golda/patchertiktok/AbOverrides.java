package com.golda.patchertiktok;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Overrides TikTok's server-side A/B experiments and settings. This is what unlocks comment
 * reposts, the new profile layout with banners and other features TikTok rolls out gradually.
 * Values are fixed when TikTok starts; changing a toggle needs a restart.
 */
final class AbOverrides {
    private static final String SETTINGS_MANAGER = "com.bytedance.ies.abmock.SettingsManager";
    /** A string only ABManager's getters use; it locates the obfuscated class in any build. */
    private static final String AB_MANAGER_FINGERPRINT = "player_setting_enable_ab_cache";

    private static final Map<String, Object> VALUES = new HashMap<>();

    private AbOverrides() { }

    static Map<String, Object> values() { return VALUES; }

    static void install(ClassLoader loader) {
        collect();
        if (VALUES.isEmpty()) return;
        int hooks = 0;
        Class<?> settings = XposedHelpers.findClassIfExists(SETTINGS_MANAGER, loader);
        if (settings != null) hooks += hookGetters(settings);
        Class<?> ab = Discovery.firstClass(loader, AB_MANAGER_FINGERPRINT);
        if (ab != null) hooks += hookGetters(ab);
        RuntimeLog.log("A/B overrides: " + VALUES.size() + " keys, " + hooks + " getters"
                + (ab == null ? " (ABManager not found)" : " in " + ab.getName()));
    }

    private static void collect() {
        if (Prefs.on(Prefs.ADS)) {
            for (String key : new String[]{"enable_normal_splash_ad", "enable_normal_splash_ad_ab", "splash_ad_enable",
                    "enable_live_splash", "aweme_splash_first_launch_enabled", "splash_show",
                    "enable_splash_show_count_for_empty", "is_enable_splash_first_show_retrieval",
                    "enable_feed_ad", "enable_video_ad", "ad_banner_enable", "commerce_enable"}) {
                VALUES.put(key, false);
            }
        }
        if (Prefs.on(Prefs.COMMENT_REPOSTS)) {
            VALUES.put("tt_comment_repost_interaction", 1);
            VALUES.put("repost_support_reply", 1);
            VALUES.put("repost_support_digg", 1);
        }
        if (Prefs.on(Prefs.NEW_PROFILE)) {
            VALUES.put("profile_bg_in_allow_list", 1);
            VALUES.put("profile_bg_enable_consumption_group", 3);
            VALUES.put("tux_profile_icon_update", 1);
            VALUES.put("profile_left_align", 1);
            VALUES.put("profile_left_align_ab_group", 11);
            VALUES.put("profile_left_align_avatar_and_info_merge", 1);
            VALUES.put("profile_left_align_small_avatar", 0);
            VALUES.put("profile_left_align_pronouns", 1);
            VALUES.put("profile_left_align_recommend_card", 1);
            for (String key : new String[]{"profile_left_align_avatar_opt", "profile_left_align_avatar_at_right_opt",
                    "profile_left_align_right_avatar_nickname_opt_v2", "enable_profile_left_align_second_line_two_lines_show",
                    "profile_left_align_relation_info_font_size_opt", "profile_left_align_right_avatar_large_font_opt",
                    "profile_left_align_bio_style_opt", "profile_left_align_cta_style_opt",
                    "profile_left_align_advance_feature_style_opt",
                    "profile_left_align_right_avatar_nickname_wordbreak_opt"}) {
                VALUES.put(key, true);
            }
        }
        if (Prefs.on(Prefs.EXTRA_FEATURES)) {
            VALUES.put("long_press_speed_up_enable", true);
            VALUES.put("long_press_speed_up_lock", 120);
            VALUES.put("long_press_quick_comment", 1);
            VALUES.put("feed_long_press_panel_support_all_type", true);
            VALUES.put("add_comments_to_favorites", 3);
            VALUES.put("enable_favorite_long_click", 2);
            VALUES.put("audio_comment_publish", 1);
            VALUES.put("comment_enable_live_photo", 1);
            VALUES.put("comment_by_shooting", 1);
            VALUES.put("comment_sort_opt_style", 1);
            VALUES.put("comment_hate_opt", 1);
            VALUES.put("tt_comment_hate_animation_opt", 2);
            VALUES.put("dm_customize_message_bubble", 1);
            VALUES.put("im_contacts_multi_select_limit", 999);
            VALUES.put("inbox_bb_archive_enable", 1);
            VALUES.put("background_play_enable", 1);
            VALUES.put("fyp_auto_scroll", 1);
            VALUES.put("feed_title_timestamp_trial", "v3");
        }
    }

    private static int hookGetters(Class<?> type) {
        int count = 0;
        for (Method method : type.getDeclaredMethods()) {
            Class<?> result = method.getReturnType();
            if (result == void.class || Modifier.isAbstract(method.getModifiers())) continue;
            int keyIndex = -1;
            Class<?>[] parameters = method.getParameterTypes();
            for (int index = 0; index < parameters.length; index++) {
                if (parameters[index] == String.class) { keyIndex = index; break; }
            }
            if (keyIndex < 0) continue;
            int key = keyIndex;
            try {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        Object name = param.args[key];
                        if (!(name instanceof String)) return;
                        Object value = VALUES.get(name);
                        if (value == null || param.hasThrowable()) return;
                        Object converted = convert(value, result, param.getResult());
                        if (converted != null) param.setResult(converted);
                    }
                });
                count++;
            } catch (Throwable ignored) { }
        }
        return count;
    }

    /** Converts an override to the getter's return type; unknown object types are left alone. */
    static Object convert(Object value, Class<?> type, Object current) {
        Class<?> target = type;
        if (type == Object.class) {
            if (current == null) return null;
            target = current.getClass();
        }
        try {
            if (target == boolean.class || target == Boolean.class) {
                if (value instanceof Boolean) return value;
                if (value instanceof Number) return ((Number) value).intValue() != 0;
                return Boolean.parseBoolean(value.toString());
            }
            if (target == int.class || target == Integer.class) {
                if (value instanceof Number) return ((Number) value).intValue();
                if (value instanceof Boolean) return (Boolean) value ? 1 : 0;
                return Integer.parseInt(value.toString());
            }
            if (target == long.class || target == Long.class) {
                if (value instanceof Number) return ((Number) value).longValue();
                if (value instanceof Boolean) return (Boolean) value ? 1L : 0L;
                return Long.parseLong(value.toString());
            }
            if (target == float.class || target == Float.class) {
                return value instanceof Number ? ((Number) value).floatValue() : Float.parseFloat(value.toString());
            }
            if (target == double.class || target == Double.class) {
                return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString());
            }
            if (target == String.class) return value.toString();
        } catch (NumberFormatException ignored) { }
        return null;
    }
}
