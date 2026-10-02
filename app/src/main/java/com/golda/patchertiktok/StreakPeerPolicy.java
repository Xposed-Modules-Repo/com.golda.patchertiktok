package com.golda.patchertiktok;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class StreakPeerPolicy {
    private StreakPeerPolicy() {
    }

    static String directPeer(long conversationType, String account, List<String> userIds) {
        if (conversationType != 1 || !validId(account) || userIds == null) return null;
        Set<String> unique = new HashSet<>();
        for (String userId : userIds) {
            if (!validId(userId)) return null;
            unique.add(userId);
        }
        if (unique.size() != 2 || !unique.remove(account)) return null;
        return unique.iterator().next();
    }

    static boolean validId(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) return false;
        try {
            return Long.parseLong(value) > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
