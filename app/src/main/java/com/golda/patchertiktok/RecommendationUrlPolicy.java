package com.golda.patchertiktok;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class RecommendationUrlPolicy {
    private RecommendationUrlPolicy() {
    }

    static boolean isFeedUrl(String url) {
        if (url == null || !url.contains("/feed")) return false;
        try {
            URI uri = new URI(url);
            return ("https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null
                    && ("/aweme/v2/feed/".equals(uri.getRawPath())
                    || "/aweme/v1/feed/".equals(uri.getRawPath())
                    || "/aweme/v2/feed".equals(uri.getRawPath())
                    || "/aweme/v1/feed".equals(uri.getRawPath()));
        } catch (Exception ignored) {
            return false;
        }
    }

    static String rewrite(String url, RecommendationProfile profile) {
        if (profile == null || !isFeedUrl(url)) return url;
        try {
            Map<String, String> overrides = profile.parameters();
            URI uri = new URI(url);
            List<String> pairs = new ArrayList<>();
            String query = uri.getRawQuery();
            if (query != null && !query.isEmpty()) {
                for (String pair : query.split("&", -1)) {
                    int separator = pair.indexOf('=');
                    String key = URLDecoder.decode(
                            separator < 0 ? pair : pair.substring(0, separator), "UTF-8");
                    boolean replace = false;
                    for (String override : overrides.keySet()) {
                        if (override.equalsIgnoreCase(key)) {
                            replace = true;
                            break;
                        }
                    }
                    // Keep unrelated encoded values byte-for-byte, including time and cursors.
                    if (!replace) pairs.add(pair);
                }
            }
            for (Map.Entry<String, String> override : overrides.entrySet()) {
                if (!override.getValue().isEmpty()) {
                    pairs.add(override.getKey() + "=" + URLEncoder.encode(override.getValue(), "UTF-8"));
                }
            }
            String rewritten = uri.getScheme() + "://" + uri.getRawAuthority()
                    + uri.getRawPath() + "?" + String.join("&", pairs);
            if (uri.getRawFragment() != null) rewritten += "#" + uri.getRawFragment();
            return rewritten;
        } catch (Exception ignored) {
            return url;
        }
    }
}
