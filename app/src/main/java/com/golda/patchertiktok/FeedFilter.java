package com.golda.patchertiktok;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/** Removes ads, LIVE and "people you may know" items from the feed and other video lists. */
final class FeedFilter {
    private static final String SNAPSHOT = "com.golda.patchertiktok.cleanFeed";
    private static final String[] SUGGESTION_METHODS = {
            "getRelationTextKey", "getRecType", "getFriendTypeStr", "getLabelInfo", "getTabText", "getText", "getKey"};
    private static final String[] SUGGESTION_FIELDS = {
            "relationTextKey", "recType", "friendTypeStr", "labelInfo", "tabText", "text", "key"};

    /** Other screens that show video lists: following, friends, favorites, reposts, playlists, nearby. */
    private static final String[][] LIST_GETTERS = {
            {"com.ss.android.ugc.aweme.feed.module.FollowingInterestFeedResponse", "getAwemeList"},
            {"com.ss.android.ugc.aweme.relation.model.MaFUserVideoListResponse", "getAwemeList"},
            {"com.ss.android.ugc.aweme.footnote.detail.repo.FootNoteFeedItemList", "getItems"},
            {"com.ss.android.ugc.aweme.forward.model.ForwardItemList", "getItems"},
            {"com.ss.android.ugc.aweme.api.ArtistMusicAwemeResponse", "getAwemeList"},
            {"com.ss.android.ugc.aweme.music.model.FanSpotlightPickedVideosResponse", "getAwemeList"},
            {"com.ss.android.ugc.aweme.mix.model.MixCandidateVideosResponse", "getMixVideos"},
            {"com.ss.android.ugc.aweme.friendstab.api.FriendsFeedResponse", "getAwemeList"},
    };

    private FeedFilter() { }

    static void install(ClassLoader loader) {
        Class<?> feed = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.FeedItemList", loader);
        if (feed != null) hookFeedItemList(feed);
        for (String[] getter : LIST_GETTERS) {
            Class<?> type = XposedHelpers.findClassIfExists(getter[0], loader);
            if (type == null) continue;
            XposedBridge.hookAllMethods(type, getter[1], new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (param.getResult() instanceof List<?>) param.setResult(filter((List<?>) param.getResult()));
                }
            });
        }
        hookAdFields(loader);
        RuntimeLog.log("feed filter installed" + (feed == null ? " (FeedItemList missing)" : ""));
    }

    private static void hookFeedItemList(Class<?> feed) {
        XposedBridge.hookAllMethods(feed, "setItems", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args.length == 0 || !(param.args[0] instanceof List<?>)) return;
                List<?> filtered = filter((List<?>) param.args[0]);
                param.args[0] = filtered;
                remember(param.thisObject, filtered);
            }
        });
        XposedBridge.hookAllMethods(feed, "getItems", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (!(param.getResult() instanceof List<?>)) return;
                List<?> items = (List<?>) param.getResult();
                Object cached = XposedHelpers.getAdditionalInstanceField(param.thisObject, SNAPSHOT);
                if (cached instanceof Snapshot && ((Snapshot) cached).matches(items)) return;
                List<?> filtered = filter(items);
                if (filtered != items) {
                    try {
                        XposedHelpers.setObjectField(param.thisObject, "items", filtered);
                    } catch (Throwable ignored) { }
                    param.setResult(filtered);
                }
                remember(param.thisObject, filtered);
            }
        });
        XposedBridge.hookAllMethods(feed, "setPreloadAds", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (Prefs.on(Prefs.ADS) && param.args.length > 0) param.args[0] = Collections.emptyList();
            }
        });
        XposedBridge.hookAllMethods(feed, "getPreloadAds", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (Prefs.on(Prefs.ADS)) param.setResult(Collections.emptyList());
            }
        });
        XposedBridge.hookAllMethods(feed, "setHasAd", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (Prefs.on(Prefs.ADS) && param.args.length > 0) param.args[0] = false;
            }
        });
        XposedBridge.hookAllMethods(feed, "isHasAd", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (Prefs.on(Prefs.ADS)) param.setResult(false);
            }
        });
    }

    /** Mid-roll and in-video ad payloads that are not separate feed items. */
    private static void hookAdFields(ClassLoader loader) {
        XC_MethodHook zero = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (!Prefs.on(Prefs.ADS)) return;
                Class<?> type = ((Method) param.method).getReturnType();
                if (type == int.class) param.setResult(0);
                else if (type == boolean.class) param.setResult(false);
                else if (!type.isPrimitive()) param.setResult(null);
            }
        };
        hookAll(loader, "com.ss.android.ugc.aweme.feed.model.AwemeRawAd", zero, "getRollType");
        hookAll(loader, "com.ss.android.ugc.aweme.commerce.AwemeCommerceStruct", zero, "getMidRollType", "isEnableMidRoll");
        hookAll(loader, "com.ss.android.ugc.aweme.feed.model.Aweme", zero, "getAdInfo", "getAdExtInfo");
    }

    private static void hookAll(ClassLoader loader, String className, XC_MethodHook hook, String... names) {
        Class<?> type = XposedHelpers.findClassIfExists(className, loader);
        if (type == null) return;
        for (String name : names) {
            try {
                XposedBridge.hookAllMethods(type, name, hook);
            } catch (Throwable ignored) { }
        }
    }

    private static void remember(Object owner, List<?> items) {
        XposedHelpers.setAdditionalInstanceField(owner, SNAPSHOT, new Snapshot(items));
    }

    private static final class Snapshot {
        private final List<?> items;
        private final int size;

        Snapshot(List<?> items) {
            this.items = items;
            this.size = items.size();
        }

        boolean matches(List<?> current) { return items == current && size == current.size(); }
    }

    static List<?> filter(List<?> items) {
        if (items == null || items.isEmpty()) return items;
        boolean ads = Prefs.on(Prefs.ADS);
        boolean live = Prefs.on(Prefs.LIVE);
        boolean suggested = Prefs.on(Prefs.ACQUAINTANCES);
        if (!ads && !live && !suggested) return items;
        ArrayList<Object> filtered = null;
        for (int index = 0; index < items.size(); index++) {
            Object item = items.get(index);
            Object aweme = unwrap(item);
            boolean remove = (ads && (isAd(item) || isAd(aweme)))
                    || (live && (isLive(item) || isLive(aweme)))
                    || (suggested && isSuggestedAcquaintance(aweme));
            if (remove) {
                if (filtered == null) {
                    filtered = new ArrayList<>(items.size());
                    for (int previous = 0; previous < index; previous++) filtered.add(items.get(previous));
                }
            } else if (filtered != null) {
                filtered.add(item);
            }
        }
        return filtered != null ? filtered : items;
    }

    private static Object unwrap(Object item) {
        if (item == null) return null;
        for (String name : new String[]{"aweme", "mAweme", "item"}) {
            Object value = Reflect.get(item, name);
            if (value != null) return value;
        }
        return item;
    }

    static boolean isAd(Object item) {
        if (item == null) return false;
        if (Reflect.getBoolean(item, "isAd") || Reflect.callBoolean(item, "isAd")
                || Reflect.callBoolean(item, "isAdAweme") || Reflect.callBoolean(item, "isSoftAd")) return true;
        if (Reflect.call(item, "getAwemeRawAd") != null) return true;
        Object commerce = Reflect.call(item, "getCommerceVideoAuthInfo");
        if (commerce != null && Reflect.callBoolean(commerce, "isPseudoAd")
                && Reflect.call(commerce, "getPseudoAdData") != null) return true;
        return present(Reflect.get(item, "awemeRawAd")) || present(Reflect.get(item, "rawAd"));
    }

    private static boolean present(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).longValue() > 0;
        return value != null;
    }

    static boolean isLive(Object item) {
        if (item == null) return false;
        Object type = Reflect.call(item, "getAwemeType");
        if (type instanceof Number && ((Number) type).intValue() == 101) return true;
        if (Reflect.getBoolean(item, "isLive") || Reflect.callBoolean(item, "isLive")
                || Reflect.callBoolean(item, "isLiveReplay")) return true;
        return equalsIgnoreCase(Reflect.get(item, "contentType"), "live")
                || equalsIgnoreCase(Reflect.get(item, "content_type"), "live")
                || equalsIgnoreCase(Reflect.get(item, "schema"), "aweme://live");
    }

    private static boolean equalsIgnoreCase(Object value, String expected) {
        return value instanceof String && expected.equalsIgnoreCase((String) value);
    }

    private static boolean isSuggestedAcquaintance(Object aweme) {
        if (aweme == null) return false;
        Object relation = Reflect.call(aweme, "getRelationRecommendInfo");
        if (relation == null) relation = Reflect.get(aweme, "relationRecommendInfo");
        boolean familiar = Reflect.callBoolean(aweme, "isFamiliar") || Reflect.getBoolean(aweme, "isFamiliar");
        if (relation != null && FeedSuggestionClassifier.shouldRemove(familiar, true, hasMarker(relation))) return true;
        Object label = Reflect.call(aweme, "getRelationLabel");
        if (label == null) label = Reflect.get(aweme, "relationLabel");
        if (hasMarker(label)) return true;
        Object feedLabel = Reflect.call(aweme, "getFeedRelationLabel");
        if (feedLabel == null) feedLabel = Reflect.get(aweme, "feedRelationLabel");
        return hasMarker(feedLabel);
    }

    private static boolean hasMarker(Object model) {
        if (model == null) return false;
        for (String name : SUGGESTION_METHODS) {
            if (FeedSuggestionClassifier.hasAcquaintanceMarker(Reflect.call(model, name))) return true;
        }
        for (String name : SUGGESTION_FIELDS) {
            if (FeedSuggestionClassifier.hasAcquaintanceMarker(Reflect.get(model, name))) return true;
        }
        return false;
    }
}
