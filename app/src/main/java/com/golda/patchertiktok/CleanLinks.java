package com.golda.patchertiktok;

import android.content.ClipData;
import android.content.ClipboardManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** Strips share-tracking parameters from TikTok links when they are copied. */
final class CleanLinks {
    private static final Pattern URL = Pattern.compile("https?://[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIKTOK_HOST = Pattern.compile(
            "https?://([a-z0-9-]+\\.)*(tiktok\\.com|tiktokv\\.com|tiktokv\\.eu|musical\\.ly)(/|$)",
            Pattern.CASE_INSENSITIVE);
    private static final Set<String> TRACKING = new HashSet<>(Arrays.asList(
            "_r", "_t", "_d", "share_item_id", "share_link_id", "share_app_id", "share_app_name",
            "share_scene", "share_uid", "share_author_id", "timestamp", "tt_from", "u_code", "ug_btm",
            "user_id", "sec_user_id", "source", "utm_source", "utm_medium", "utm_campaign", "utm_term",
            "utm_content", "enter_from", "enter_method", "iid", "device_id", "did", "aid", "web_id", "webid",
            "mstoken", "region", "sender_device", "sender_web_id", "is_from_webapp", "is_copy_url",
            "checksum", "sec_uid", "social_share_type", "language", "refer"));

    private CleanLinks() { }

    static void install() {
        Hooks.hookAll(ClipboardManager.class, "setPrimaryClip", new Hooks.Hook() {
            @Override protected void before(Hooks.Call param) {
                if (!Prefs.on(Prefs.CLEAN_LINKS) || param.args.length == 0
                        || !(param.args[0] instanceof ClipData)) return;
                ClipData clip = (ClipData) param.args[0];
                List<String> cleaned = new ArrayList<>(clip.getItemCount());
                boolean changed = false;
                for (int index = 0; index < clip.getItemCount(); index++) {
                    CharSequence text = clip.getItemAt(index).getText();
                    if (text == null) return;
                    String value = cleanText(text.toString());
                    changed |= !value.contentEquals(text);
                    cleaned.add(value);
                }
                if (!changed) return;
                ClipData replacement = new ClipData(clip.getDescription(), new ClipData.Item(cleaned.get(0)));
                for (int index = 1; index < cleaned.size(); index++) {
                    replacement.addItem(new ClipData.Item(cleaned.get(index)));
                }
                param.args[0] = replacement;
            }
        });
    }

    static String cleanText(String text) {
        Matcher matcher = URL.matcher(text);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) matcher.appendReplacement(out, Matcher.quoteReplacement(cleanUrl(matcher.group())));
        matcher.appendTail(out);
        return out.toString();
    }

    static String cleanUrl(String url) {
        if (!TIKTOK_HOST.matcher(url).find()) return url;
        int query = url.indexOf('?');
        if (query < 0) return url;
        int hash = url.indexOf('#', query);
        String base = url.substring(0, query);
        String params = hash < 0 ? url.substring(query + 1) : url.substring(query + 1, hash);
        String fragment = hash < 0 ? "" : url.substring(hash);
        StringBuilder kept = new StringBuilder();
        for (String pair : params.split("&")) {
            if (pair.isEmpty()) continue;
            int equals = pair.indexOf('=');
            String key = (equals < 0 ? pair : pair.substring(0, equals)).toLowerCase(Locale.ROOT);
            if (TRACKING.contains(key)) continue;
            kept.append(kept.length() == 0 ? '?' : '&').append(pair);
        }
        return base + kept + fragment;
    }
}
